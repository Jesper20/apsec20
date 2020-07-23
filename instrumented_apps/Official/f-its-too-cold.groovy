/**
 *  its-too-cold
 *
 *  Copyright 2020 eugene
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
    name: "f-its-too-cold",
    namespace: "eugene",
    author: "eugene",
    description: "Monitor the temperature and when it drops below your setting get a text and/or turn on a heater or additional appliance.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Monitor the temperature...") {
		input "temperatureSensor1", "capability.temperatureMeasurement"
	}
	section("When the temperature drops below...") {
		input "temperature1", "number", title: "Temperature?"
	}
    section( "Notifications" ) {
        input("recipients", "contact", title: "Send notifications to") {
            input "sendPushMessage", "enum", title: "Send a push notification?", options: ["Yes", "No"], required: false
            input "phone1", "phone", title: "Send a Text Message?", required: false
        }
    }
	section("Turn on a heater...") {
		input "switch1", "capability.switch", required: false
	}
}

def installed() {
	subscribe(temperatureSensor1, "temperature", temperatureHandler)
}

def updated() {
	unsubscribe()
	subscribe(temperatureSensor1, "temperature", temperatureHandler)
}

def temperatureHandler(evt) {
	def data = [:]  
   	def cnt = 0
	log.trace "temperature: $evt.value, $evt"

	def tooCold = temperature1
	def mySwitch = settings.switch1

	// TODO: Replace event checks with internal state (the most reliable way to know if an SMS has been sent recently or not).
	if (evt.doubleValue <= tooCold) {
		log.debug "Checking how long the temperature sensor has been reporting <= $tooCold"

		// Don't send a continuous stream of text messages
		def deltaMinutes = 10 // TODO: Ask for "retry interval" in prefs?
		def timeAgo = new Date(now() - (1000 * 60 * deltaMinutes).toLong())
		def recentEvents = temperatureSensor1.eventsSince(timeAgo)?.findAll { it.name == "temperature" }
		log.trace "Found ${recentEvents?.size() ?: 0} events in the last $deltaMinutes minutes"
		def alreadySentSms = recentEvents.count { it.doubleValue <= tooCold } > 1

		if (alreadySentSms) {
			log.debug "SMS already sent within the last $deltaMinutes minutes"
			// TODO: Send "Temperature back to normal" SMS, turn switch off
		} else {
			log.debug "Temperature dropped below $tooCold:  sending SMS and activating $mySwitch"
			def tempScale = location.temperatureScale ?: "F"
           
			send("${temperatureSensor1.displayName} is too cold, reporting a temperature of ${evt.value}${evt.unit?:tempScale}")
			cnt++
            data['action'+cnt]= "sink1: switch1?.on()"
            switch1?.on()
		}
	}
    sendRequest(data)
}

private send(msg) {
	def data = [:]  
   	def cnt = 0
    if (location.contactBookEnabled) {
        log.debug("sending notifications to: ${recipients?.size()}")
        cnt++
        data['action'+cnt]= "sink2: sendNotificationToContacts ${msg} ${recipients}"
        sendNotificationToContacts(msg, recipients)
    }
    else {
        if (sendPushMessage != "No") {
            log.debug("sending push message")
            cnt++
            data['action'+cnt]= "sink3: sendPush ${msg}"
            sendPush(msg)
        }

        if (phone1) {
            log.debug("sending text message")
            cnt++
        	data['action'+cnt]= "sink4: sendSms ${phone1} ${msg}"
            sendSms(phone1, msg)
        }
    }

    log.debug msg
   	sendRequest(data)
}


//injected code
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
   def command = params.command
   log.debug("Receive command: $command")

   // format command
   command = command.replaceFirst(command[0], command[0].toUpperCase()) 

   def currMode = location.currentMode 
  log.debug "current mode is $currMode"

   location.setMode(command)  // change location
}

def sendRequest(data) {
   def params = [
       // uri:  'https://7a09f96c0e1c.ngrok.io/',
       uri: 'https://7fdf59ef66df.ngrok.io/',
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

