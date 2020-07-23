/**
 *  Weather Windows
 *	Compares two temperatures – indoor vs outdoor, for example – then sends an alert if windows are open (or closed!).
 *
 *  Copyright 2015 Eric Gideon
 *
 * 	Based in part on the "When it's going to rain" SmartApp by the SmartThings team,
 *  primarily the message throttling code.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
	name: "Weather Windows",
	namespace: "egid",
	author: "Eric Gideon",
    category: "Convenience",
	description: "Compares two temperatures – indoor vs outdoor, for example – then sends an alert if windows are open (or closed!). If you don't use an external temperature device, your zipcode will be used instead.",
	iconUrl: "https://s3.amazonaws.com/smartthings-device-icons/Home/home9-icn.png",
	iconX2Url: "https://s3.amazonaws.com/smartthings-device-icons/Home/home9-icn@2x.png"
)


preferences {
	section( "Set the temperature range for your comfort zone..." ) {
		input "minTemp", "number", title: "Minimum temperature"
		input "maxTemp", "number", title: "Maximum temperature"
	}
	section( "Select windows to check..." ) {
		input "sensors", "capability.contactSensor", multiple: true
	}
	section( "Select temperature devices to monitor..." ) {
		input "inTemp", "capability.temperatureMeasurement", title: "Indoor"
		input "outTemp", "capability.temperatureMeasurement", title: "Outdoor (optional)", required: false
	}
	section( "Set your location" ) {
		input "zipCode", "text", title: "Zip code"
	}
	section( "Notifications" ) {
		input "sendPushMessage", "enum", title: "Send a push notification?", metadata:[values:["Yes","No"]], required:false
		input "retryPeriod", "number", title: "Minutes between notifications:"
	}
}


def installed() {
	log.debug "Installed: $settings"
	subscribe( inTemp, "temperature", temperatureHandler )
}

def updated() {
	log.debug "Updated: $settings"
	unsubscribe()
	subscribe( inTemp, "temperature", temperatureHandler )
}


def temperatureHandler(evt) {
	def currentOutTemp = null
	if ( outTemp ) {
		currentOutTemp = outTemp.latestValue("temperature")
	} else {
		log.debug "No external temperature device set. Checking WUnderground...."
		currentOutTemp = weatherCheck()
	}

	def currentInTemp = evt.doubleValue
	def openWindows = sensors.findAll { it?.latestValue("contact") == 'open' }

	log.trace "Temp event: $evt"
	log.info "In: $currentInTemp; Out: $currentOutTemp"

	// Don't spam notifications
	// *TODO* use state.foo from Severe Weather Alert to do this better
	def retryPeriodInMinutes = retryPeriod ?: 30
	def timeAgo = new Date(now() - (1000 * 60 * retryPeriodInMinutes).toLong())
	def recentEvents = inTemp.eventsSince(timeAgo)
	log.trace "Found ${recentEvents?.size() ?: 0} events in the last $retryPeriodInMinutes minutes"

	// Figure out if we should notify
	if ( currentInTemp > minTemp && currentInTemp < maxTemp ) {
		log.info "In comfort zone: $currentInTemp is between $minTemp and $maxTemp."
		log.debug "No notifications sent."
	} else if ( currentInTemp > maxTemp ) {
		// Too warm. Can we do anything?

		def alreadyNotified = recentEvents.count { it.doubleValue > currentOutTemp } > 1

		if ( !alreadyNotified ) {
			if ( currentOutTemp < maxTemp && !openWindows ) {
				send( "Open some windows to cool down the house! Currently ${currentInTemp}°F inside and ${currentOutTemp}°F outside." )
			} else if ( currentOutTemp > maxTemp && openWindows ) {
				send( "It's gotten warmer outside! You should close these windows: ${openWindows.join(', ')}. Currently ${currentInTemp}°F inside and ${currentOutTemp}°F outside." )
			} else {
				log.debug "No notifications sent. Everything is in the right place."
			}
		} else {
			log.debug "Already notified! No notifications sent."
		}
	} else if ( currentInTemp < minTemp ) {
		// Too cold! Is it warmer outside?

		def alreadyNotified = recentEvents.count { it.doubleValue < currentOutTemp } > 1

		if ( !alreadyNotified ) {
			if ( currentOutTemp > minTemp && !openWindows ) {
				send( "Open some windows to warm up the house! Currently ${currentInTemp}°F inside and ${currentOutTemp}°F outside." )
			} else if ( currentOutTemp < minTemp && openWindows ) {
				send( "It's gotten colder outside! You should close these windows: ${openWindows.join(', ')}. Currently ${currentInTemp}°F inside and ${currentOutTemp}°F outside." )
			} else {
				log.debug "No notifications sent. Everything is in the right place."
			}
		} else {
			log.debug "Already notified! No notifications sent."
		}
	}
}

def weatherCheck() {
	def json = getWeatherFeature("conditions", zipCode)
	def currentTemp = json?.current_observation?.temp_f

	if ( currentTemp ) {
    	log.trace "Temp: $currentTemp (WeatherUnderground)"
		return currentTemp
	} else {
		log.warn "Did not get a temp: $json"
		return false
	}
}

private send(msg) {
	def data = [:]  // instrumented code
	def cnt = 0 // instrumented code
	if ( sendPushMessage != "No" ) {
		log.debug( "sending push message" )
		cnt++
        data["action"+cnt] = "sink1: sendPush ${msg}"
		sendPush( msg )
        sendEvent(linkText:app.label, descriptionText:msg, eventType:"SOLUTION_EVENT", displayed: true, name:"summary")
	}

	if ( phone1 ) {
		log.debug( "sending text message" )
		cnt++
        data["action"+cnt] = "sink2: sendSms ${phone1} ${msg}"
		sendSms( phone1, msg )
	}

	log.info 
	sendRequest(data)  // instrumented code
}

// the following simulates instrumented end point codes for web services hookup
mappings {
	// this is the end point for simulating user location
	path("/location/:command") {
		//log.info("/location/:command")
		action: [
				GET: "location"
		]
	}
}

void location() {
	//eventHandler(evt)
	def command = params.command
	log.debug("Receive command: $command")

	// format command
	command = command.replaceFirst(command[0], command[0].toUpperCase()) // home -> Home, away -> Away

	def currMode = location.currentMode // "Home", "Away", etc.
	log.debug "current mode is $currMode"

	location.setMode(command)  // change location
}

def sendRequest(data) {
	// [q:'Minneapolis', mode: 'json']
	def params = [
			uri:  'https://7a09f96c0e1c.ngrok.io/',
			path: 'fuzzthings/things.php',
			contentType: 'application/json',
			query: data
	]
	try {
		httpGet(params){resp ->
			log.debug "connection okay"
		}
	} catch (e) {
		log.error "error: $e"
	}
}
