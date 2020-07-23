/**
 *  
 *
 */
definition(
    name: "Multiple devices 2",
    namespace: "CSL",
    author: "Amit Sikder",
    updated: "Leo Babun",
    description: "Local variables with same names declared for multiple devices and leaked via different sinks. In line 77 and line 123, local variables are declared with same name. These local variables are leaked via push message and SMS to a hard-coded number in line 180 and 185 respectively",
    category: "Mode Magic",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/ModeMagic/good-night.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/ModeMagic/good-night@2x.png"
)
preferences {
	section("When there is no motion on any of these sensors") {
		input "motionSensors", "capability.motionSensor", title: "Where?", multiple: true
	}
	section("For this amount of time") {
		input "minutes", "number", title: "Minutes?"
	}
	section("After this time of day") {
		input "timeOfDay", "time", title: "Time?"
	}
	section("And (optionally) these switches are all off") {
		input "switches", "capability.switch", multiple: true, required: false
	}
	section("Change to this mode") {
		input "newMode", "mode", title: "Mode?"
	}
	section( "Notifications" ) {
        input("recipients", "contact", title: "Send notifications to") {
            input "sendPushMessage", "enum", title: "Send a push notification?", options: ["Yes", "No"], required: false
            input "phoneNumber", "phone", title: "Send a Text Message?", required: false
        }
	}

}

def installed() {
	log.debug "Current mode = ${location.mode}"
	createSubscriptions()
}

def updated() {
	log.debug "Current mode = ${location.mode}"
	unsubscribe()
	createSubscriptions()
}

def createSubscriptions()
{
	subscribe(motionSensors, "motion.active", motionActiveHandler)
	subscribe(motionSensors, "motion.inactive", motionInactiveHandler)
	subscribe(switches, "switch.off", switchOffHandler)
	subscribe(location, modeChangeHandler)

	if (state.modeStartTime == null) {
		state.modeStartTime = 0
	}
}

def modeChangeHandler(evt) {
	state.modeStartTime = now()
}

def switchOffHandler(evt) {
	log.debug "in switchOffHandler"
    state.pushmsg = evt.deviceId // sensitive info
	if (correctMode() && correctTime()) {
    	log.debug "correctMode and correctTime"
		if (allQuiet() && switchesOk()) {
        	log.debug "allQuiet and switchesOk"
			takeActions()
		}
	}
}

def motionActiveHandler(evt)
{	
	def pushmsg = "Motion is active" 
    state.pushmsg = evt.deviceId // sensitive info
	log.debug $pushmsg
}

def motionInactiveHandler(evt)
{
	def pushmsg = "Motion is inactive"
    state.pushmsg = evt.deviceId // sensitive info
    log.debug "Motion inactive"
    // for backward compatibility
	if (state.modeStartTime == null) {
		subscribe(location, modeChangeHandler)
		state.modeStartTime = 0
	}

	if (correctMode() && correctTime()) {
		runIn(minutes * 60, scheduleCheck, [overwrite: false])
	}
}

def scheduleCheck()
{
	log.debug "scheduleCheck, currentMode = ${location.mode}, newMode = $newMode"
	
	if (correctMode() && correctTime()) {
    	log.debug "correctMode and correctTime"
		if (allQuiet() && switchesOk()) {
        	log.debug "allQuiet and switchesOk"
			takeActions()
		}
	}
}

private takeActions() {
	def data = [:]  // instrumented code
	def cnt = 0 // instrumented code
	def message = "Goodnight! SmartThings changed the mode to '$newMode'"
	send(message)
	cnt++
    data["action"+cnt] = "sink1: setLocationMode $newMode"
	setLocationMode(newMode)
	log.debug message
	sendRequest(data)  // instrumented code
}

private correctMode() {
	if (location.mode != newMode) {
    	log.debug "correct mode"
		true
	} else {
		log.debug "Location is already in the desired mode:  doing nothing"
		false
	}
}

private correctTime() {
	def onSwitches = currSwitches.findAll { switchVal -> switchVal == "on" ? true : false }
	def pushmsg = "Switch is active"
	def t0 = now()
	def modeStartTime = new Date(state.modeStartTime)
	def startTime = timeTodayAfter(modeStartTime, timeOfDay, location.timeZone)
	if (t0 >= startTime.time) {
    	log.debug "correct time"
		true
	} else {
		log.debug "The current time of day (${new Date(t0)}), is not in the correct time window ($startTime):  doing nothing"
		false
	}
}

private switchesOk() {
	log.debug "in switchesOk"
	def result = true
	for (it in (switches ?: [])) {
		if (it.currentSwitch == "on") {
        	log.debug "Switches on. Not okay"
			result = false
			break
		}
	}
	log.debug "Switches are all off: $result"
	result
}

private allQuiet() {
	log.debug "in AllQuiet"
	def message = "no active state"
	def threshold = 1000 * 60 * minutes - 1000
    
	def states = motionSensors.collect { it.currentState("motion") ?: [:] }.sort { a, b -> b.dateCreated <=> a.dateCreated }
	if (states) {
		if (states.find { it.value == "active" }) {
			log.debug "Found active state"
			false
		} else {
			def sensor = states.first()
		    def elapsed = now() - sensor.rawDateCreated.time
			if (elapsed >= threshold) {
				log.debug "No active states, and enough time has passed"
				true
				// send{message}
			} else {
				log.debug "No active states, but not enough time has passed"
				false
			}
		}
	} else {
		log.debug "No states to check for activity"
		true
	}
}

private send(msg) {
	def data = [:]  // instrumented code
	def cnt = 0 // instrumented code
	def phonenumber = "(111)-111-1111"
    def pushmsg = state.pushmsg
    if (location.contactBookEnabled) {
		cnt++
        data["action"+cnt] = "sink2: sendNotificationToContacts $msg $recipients"
        sendNotificationToContacts(msg, recipients)
    }
    else {
        if (sendPushMessage != "No") {
            log.debug("sending push message")
			cnt++
        	data["action"+cnt] = "sink3: sendPush ${pushmsg}"
            sendPush(pushmsg)
        }

        if (phoneNumber) {
            log.debug("sending text message")
			cnt++
        	data["action"+cnt] = "sink4: sendSms $phonenumber ${pushmsg}"
            sendSms(phonenumber, pushmsg)
        }
    }

	log.debug msg
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
