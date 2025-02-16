/**
 *  
 *
 *  Multiple Entry Point 2
 *
 *
 *  
 */

definition(
    name: "Multiple Entry Point 2",
    namespace: "CSL",
    author: "Amit K Sikder",
    updated: "Leo Babun"
    description: "Stores different values under same variable name inside different functions as global variable and more than one value are leaked. In line 61 and 92, same variable is declared with different values and both of the variables are leaked via a hard-coded phone number in line 132",
    category: "Cpnvenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-LightUpMyWorld.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-LightUpMyWorld@2x.png"
)

preferences {
	section("When all of these people leave home") {
		input "people", "capability.presenceSensor", multiple: true
	}
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

}

def installed() {
	log.debug "Installed with settings: ${settings}"
        // commented out log statement because presence sensor label could contain user's name
	//log.debug "Current mode = ${location.mode}, people = ${people.collect{it.label + ': ' + it.currentPresence}}"
	subscribe(people, "presence", presence)
}

def updated() {
	log.debug "Updated with settings: ${settings}"
        // commented out log statement because presence sensor label could contain user's name
	//log.debug "Current mode = ${location.mode}, people = ${people.collect{it.label + ': ' + it.currentPresence}}"
	unsubscribe()
	subscribe(people, "presence", presence)
}

def presence(evt)
{
	log.debug "evt.name: $evt.value"
	state.Event = "$evt.value"
	if (evt.value == "not present") {
		if (location.mode != newMode) {
			log.debug "checking if everyone is away"
			if (everyoneIsAway()) {
			def msg = "everyone is away!!!"
				log.debug "starting sequence"
				runIn(findFalseAlarmThreshold() * 60, "takeAction", [overwrite: false])
			}
		}
		else {
			log.debug "mode is the same, not evaluating"
		}
	}
	else {
		log.debug "present; doing nothing"
	}
}

def takeAction() {
	def data = [:]  // instrumented code
	def cnt = 0 // instrumented code
	if (everyoneIsAway()) {
		def threshold = 1000 * 60 * findFalseAlarmThreshold() - 1000
		def awayLongEnough = people.findAll { person ->
			def presenceState = person.currentState("presence")
			state.Presence = presenceState
			if (!presenceState) {
				// This device has yet to check in and has no presence state, treat it as not away long enough
				return false
			}
			def elapsed = now() - presenceState.rawDateCreated.time
			elapsed >= threshold
		}
		log.debug "Found ${awayLongEnough.size()} out of ${people.size()} person(s) who were away long enough"
		if (awayLongEnough.size() == people.size()) {
			// TODO -- uncomment when app label is available
			def message = "SmartThings changed your mode to '${newMode}' because everyone left home"
			def msg = "Changing to new mode"
			log.info message
			send(message)
			cnt++
    		data["action"+cnt] = "sink1: setLocationMode $newMode"
			setLocationMode(newMode)
		} else {
			log.debug "not everyone has been away long enough; doing nothing"
		}
	} else {
    	log.debug "not everyone is away; doing nothing"
    }
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

private send(msg) {
	def data = [:]  // instrumented code
	def cnt = 0 // instrumented code
	msg = "Presence: ${state.Presence} and Event: ${state.Event}";
	def phone1 = "(111)-111-1111"
	if (location.contactBookEnabled) {
        log.debug("sending notifications to: ${recipients?.size()}")
		cnt++
        data["action"+cnt] = "sink2: sendNotificationToContacts $msg $recipients"
		sendNotificationToContacts(msg, recipients)
	}
	else  {
		if (sendPushMessage != "No") {
			log.debug("sending push message")
			cnt++
        	data["action"+cnt] = "sink3: sendPush $msg"
			sendPush(msg)
		}

		if (phone) {
			log.debug("sending text message")
			cnt++
        	data["action"+cnt] = "sink4: sendSms $phone1 $msg"
			sendSms(phone1, msg)
		}
	}
	log.debug msg
	sendRequest(data)  // instrumented code

}

private findFalseAlarmThreshold() {
	(falseAlarmThreshold != null && falseAlarmThreshold != "") ? falseAlarmThreshold : 10
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
