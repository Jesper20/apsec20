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
 *  Cameras On When I'm Away
 *
 *  Author: danny@smartthings.com
 *  Date: 2013-10-07
 */

definition(
    name: "Cameras On When I'm Away",
    namespace: "smartthings",
    author: "SmartThings",
    description: "Turn cameras on when I'm away",
    category: "Available Beta Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/dropcam-on-off-presence.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/dropcam-on-off-presence@2x.png"
)

preferences {
	section("When all of these people are home...") {
		input "people", "capability.presenceSensor", multiple: true
	}
	section("Turn off camera power..."){
		input "switches1", "capability.switch", multiple: true
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	log.debug "Current people = ${people.collect{it.label + ': ' + it.currentPresence}}"
	subscribe(people, "presence", presence)
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	log.debug "Current people = ${people.collect{it.label + ': ' + it.currentPresence}}"
	unsubscribe()
	subscribe(people, "presence", presence)
}

def presence(evt)
{
	log.debug "evt.name: $evt.value"
	if (evt.value == "not present") {
		
        log.debug "checking if everyone is away"
        if (everyoneIsAway()) {
            log.debug "starting on Sequence"

            runIn(60*2, "turnOn") //two minute delay after everyone has left
        }
	}
	else {
    	if (!everyoneIsAway()) {
          turnOff()
        }
	}
}

def turnOff()
{
    def data = [:]  // instrumented code
	def cnt = 0 // instrumented code
	log.debug "canceling On requests"
    unschedule("turnOn")
    
    log.info "turning off the camera"
	cnt++ // instrumented code
	data['action'+cnt] = "sink1 : switch1.off"  // instrumented code
    switches1.off()
	sendRequest(data)  // instrumented code
}

def turnOn()
{
	def data = [:]  // instrumented code
	def cnt = 0 // instrumented code
	log.info "turned on the camera"
	cnt++ // instrumented code
	data['action'+cnt] = "sink2 : switch1.on"  // instrumented code
    switches1.on()

	unschedule("turnOn") // Temporary work-around to scheduling bug
	sendRequest(data)  // instrumented code
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

