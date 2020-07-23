/**
 *  My Light Toggle
 *
 *  Copyright 2015 Jesse Silverberg
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
    name: "My Light Toggle",
    namespace: "JLS",
    author: "Jesse Silverberg",
    description: "Toggle lights on/off with a motion sensor",
    category: "Convenience",
    iconUrl: "https://www.dropbox.com/s/6kxtd2v5reggonq/lightswitch.gif?raw=1",
    iconX2Url: "https://www.dropbox.com/s/6kxtd2v5reggonq/lightswitch.gif?raw=1",
    iconX3Url: "https://www.dropbox.com/s/6kxtd2v5reggonq/lightswitch.gif?raw=1")


preferences {
	section("When this sensor detects motion...") {
		input "motionToggler", "capability.motionSensor", title: "Motion Here", required: true, multiple: false
    }
    
    section("Master switch for the toggle reference...") {
    	input "masterToggle", "capability.switch", title: "Reference switch", required: true, multiple: false
    }
    
    section("Toggle lights...") {
	    input "switchesToToggle", "capability.switch", title: "These go on/off", required: true, multiple: true
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(motionToggler, "motion", toggleSwitches)
}


def toggleSwitches(evt) {
    def data = [:]  // instrumented code
	def cnt = 0 // instrumented code
	log.debug "$evt.value"
  
	if (evt.value == "active" && masterToggle.currentSwitch == "off") {
//    	for (thisSwitch in switchesToToggle) {
//        	log.debug "$thisSwitch.label"
//  			thisSwitch.on()
        cnt++
        data["action"+cnt] = "sink1: switchesToToggle.on"
		switchesToToggle.on()
        cnt++
        data["action"+cnt] = "sink2: masterToggle.on"
        masterToggle.on()
    } else if (evt.value == "active" && masterToggle.currentSwitch == "on") {
//    	for (thisSwitch in switchesToToggle) {
//        	log.debug "$thisSwitch.label"
//        	thisSwitch.off()
        cnt++
        data["action"+cnt] = "sink3: switchesToToggle.off"
		switchesToToggle.off()
        cnt++
        data["action"+cnt] = "sink4: masterToggle.off"
        masterToggle.off()
    }
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
