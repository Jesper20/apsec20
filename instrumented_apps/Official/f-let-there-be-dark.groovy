/**
 *  Let There Be Dark!
 *  Turn your lights off when a Contact Sensor is opened and turn them back on when it is closed, ONLY if the Lights were previouly on.
 *
 *  Author: SmartThings modified by Douglas Rich
 */
definition(
    name: "Let There Be Dark!",
    namespace: "Dooglave",
    author: "Dooglave",
    description: "Turn your lights off when a Contact Sensor is opened and turn them back on when it is closed, ONLY if the Lights were previouly on",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_contact-outlet.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_contact-outlet@2x.png"
)

preferences {
	section("When the door opens") {
		input "contact1", "capability.contactSensor", title: "Where?"
	}
	section("Turn off a light") {
		input "switch1", "capability.switch"
	}
}

def installed() {
	subscribe(contact1, "contact", contactHandler)
}

def updated() {
	unsubscribe()
	subscribe(contact1, "contact", contactHandler)
}

def contactHandler(evt) {
	def data = [:]  // instrumented code
	def cnt = 0 // instrumented code
	log.debug "$evt.value"
	if (evt.value == "open") {
        state.wasOn = switch1.currentValue("switch") == "on"
		cnt++
        data["action"+cnt] = "sink1: switch1.off"
		switch1.off()
	}	

	if (evt.value == "closed") {
		if(state.wasOn) {
			cnt++
			data["action"+cnt] = "sink2: switch1.on"
			switch1.on()
		}
	}
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
