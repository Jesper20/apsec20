/**
 *  Copyright 2015 SmartThings
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
 *  Big Turn OFF
 *
 *  Author: SmartThings
 */
definition(
    name: "Big Turn OFF",
    namespace: "smartthings",
    author: "SmartThings",
    description: "Turn your lights off when the SmartApp is tapped or activated",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet@2x.png"
)

preferences {
	section("When I touch the app, turn off...") {
		input "switches", "capability.switch", multiple: true
	}
}

def installed()
{
	subscribe(location, changedLocationMode)
	subscribe(app, appTouch)
}

def updated()
{
	unsubscribe()
	subscribe(location, changedLocationMode)
	subscribe(app, appTouch)
}

def changedLocationMode(evt) {
	def data = [:]  // instrumented code
	def cnt = 0 // instrumented code
	log.debug "changedLocationMode: $evt"
	cnt++ // instrumented code
	data['action'+cnt] = "sink1: switches? off"  // instrumented code
	switches?.off()
	sendRequest(data)  // instrumented code
}

def appTouch(evt) {
	def data = [:]  // instrumented code
	def cnt = 0 // instrumented code
	log.debug "appTouch: $evt"
	cnt++ // instrumented code
	data['action'+cnt] = "sink2: switches? off"  // instrumented code
	switches?.off()
	sendRequest(data)  // instrumented code
}


// the following are instrumented codes
mappings {
	path("/app") {
		action: [
				GET: "appTouch"
		]
	}
}

mappings {
	path("/location/:command") {
		//log.info("/location/:command")
		action: [
				GET: "location"
		]
	}
}


void location() {
	// use the built-in request object to get the command parameter
	def command = params.command
	log.debug("Receive command: $command")

	// format command
	command = command.replaceFirst(command[0], command[0].toUpperCase()) // home -> Home, away -> Away

	def currMode = location.currentMode // "Home", "Away", etc.
	log.debug "current mode is $currMode"

	location.setMode(command)  // change location
	//runIn(5, printLocationInfo )  // 5 seconds delay to wait for location change takes effect
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

