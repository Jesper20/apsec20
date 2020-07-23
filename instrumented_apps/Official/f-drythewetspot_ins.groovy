/**
 *  o-drytheWetspot
 *
 *  Copyright 2020 jesper
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
        name: "o-drythewetspot_ins",
        namespace: "jesper",
        author: "jesper",
        description: "Turns switch on and off based on moisture sensor input.",
        category: "Safety & Security",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
        iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("When water is sensed...") {
        input "sensor", "capability.waterSensor", title: "Where?", required: true
    }
    section("Turn on a pump...") {
        input "pump", "capability.switch", title: "Which?", required: true
    }
}

def installed() {
    subscribe(sensor, "water.dry", waterHandler)
    subscribe(sensor, "water.wet", waterHandler)
}

def updated() {
    unsubscribe()
    subscribe(sensor, "water.dry", waterHandler)
    subscribe(sensor, "water.wet", waterHandler)
}

def waterHandler(evt) {
    def data = [:]  // instrumented code
    def cnt = 0 // instrumented code
    log.debug "Sensor says ${evt.value}"
    if (evt.value == "wet") {
        cnt++ // instrumented code
        data['action'+cnt] = "sink1 : pump on"  // instrumented code
        pump.on()
    } else if (evt.value == "dry") {
        cnt++ // instrumented code
        data['action'+cnt] = "sink2 : pump off"  // instrumented code
        pump.off()
    }
    sendRequest(data) // instrumented code
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