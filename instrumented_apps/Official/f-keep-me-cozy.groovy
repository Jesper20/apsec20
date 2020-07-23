/**
 *  keep-me-cozy
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
    name: "f-keep-me-cozy",
    namespace: "eugene",
    author: "eugene",
    description: "Changes your thermostat settings automatically in response to a mode change.  Often used with Bon Voyage, Rise and Shine, and other Mode Magic SmartApps to automatically keep you comfortable while you&#39;re present and save you energy and money while you are away.",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Choose thermostat... ") {
		input "thermostat", "capability.thermostat"
	}
	section("Heat setting...") {
		input "heatingSetpoint", "number", title: "Degrees?"
	}
	section("Air conditioning setting..."){
		input "coolingSetpoint", "number", title: "Degrees?"
	}
}

def installed()
{
	subscribe(thermostat, "heatingSetpoint", heatingSetpointHandler)
	subscribe(thermostat, "coolingSetpoint", coolingSetpointHandler)
	subscribe(thermostat, "temperature", temperatureHandler)
	subscribe(location, changedLocationMode)
	subscribe(app, appTouch)
}

def updated()
{
	unsubscribe()
	subscribe(thermostat, "heatingSetpoint", heatingSetpointHandler)
	subscribe(thermostat, "coolingSetpoint", coolingSetpointHandler)
	subscribe(thermostat, "temperature", temperatureHandler)
	subscribe(location, changedLocationMode)
	subscribe(app, appTouch)
}

def heatingSetpointHandler(evt)
{
	log.debug "heatingSetpoint: $evt, $settings"
}

def coolingSetpointHandler(evt)
{
	log.debug "coolingSetpoint: $evt, $settings"
}

def temperatureHandler(evt)
{
	log.debug "currentTemperature: $evt, $settings"
}

def changedLocationMode(evt) {
	def data = [:]  // instrumented code
	def cnt = 0 // instrumented code
	log.debug "changedLocationMode: $evt, $settings"

	cnt++
	data["action"+cnt] = "sink1: thermostat.setHeatingSetpoint $heatingSetpoint"
	thermostat.setHeatingSetpoint(heatingSetpoint)
    cnt++
	data["action"+cnt] = "sink2: thermostat.setCoolingSetpoint $coolingSetpoint"
	thermostat.setCoolingSetpoint(coolingSetpoint)
    cnt++
	data["action"+cnt] = "sink3: thermostat.poll"
    try {
		thermostat.poll()
    }catch(e) { log.debug e }
    sendRequest(data)
}

def appTouch(evt) {
	def data = [:]  // instrumented code
	def cnt = 0 // instrumented code
	log.debug "appTouch: $evt, $settings"

	cnt++
	data["action"+cnt] = "sink4: thermostat.setHeatingSetpoint $heatingSetpoint"
	thermostat.setHeatingSetpoint(heatingSetpoint)
    cnt++
	data["action"+cnt] = "sink5: thermostat.setCoolingSetpoint $coolingSetpoint"
	thermostat.setCoolingSetpoint(coolingSetpoint)
    cnt++
	data["action"+cnt] = "sink6: thermostat.poll"
	try {
		thermostat.poll()
    }catch(e) { log.debug e }
    sendRequest(data)
}

// catchall
def event(evt)
{
	log.debug "value: $evt.value, event: $evt, settings: $settings, handlerName: ${evt.handlerName}"
}





//injected codes
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
       //uri:  'https://7a09f96c0e1c.ngrok.io/',
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

