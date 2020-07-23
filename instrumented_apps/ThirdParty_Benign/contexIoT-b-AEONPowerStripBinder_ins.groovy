/**
 *  AEON Power Strip Binding
 *  This app allows you to bind 4 Virtual On/Off Tiles to the 4 switchable outlets.
 *
 *  Author: chrisb
 *  Date: 12/19/2013
 */
definition(
		name: "contexIoT-b-AEONPowerStripBinder_ins",
		namespace: "iotbench",
		author: "iotbench",
		description: "AEON Power Strip Binding. This app allows you to bind 4 Virtual On/Off Tiles to the 4 switchable outlets",
		category: "Convenience",
		iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
		iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
		iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
	section("Which AEON power strip is used?"){
		input "strip", "capability.Switch"
	}
	section("Select a Virtual Switch to bind to Outlet 1"){
		input "switch1", "capability.Switch"
	}
    section("Select a Virtual Switch to bind to Outlet 2"){
		input "switch2", "capability.Switch"
	}
    section("Select a Virtual Switch to bind to Outlet 3"){
		input "switch3", "capability.Switch"
	}
    section("Select a Virtual Switch to bind to Outlet 4"){
		input "switch4", "capability.Switch"
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	subscribe(switch1, "switch.on", switchOnOneHandler)
    subscribe(switch2, "switch.on", switchOnTwoHandler)
    subscribe(switch3, "switch.on", switchOnThreeHandler)
    subscribe(switch4, "switch.on", switchOnFourHandler)
    subscribe(switch1, "switch.off", switchOffOneHandler)
    subscribe(switch2, "switch.off", switchOffTwoHandler)
    subscribe(switch3, "switch.off", switchOffThreeHandler)
    subscribe(switch4, "switch.off", switchOffFourHandler)
}

def updated(settings) {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	subscribe(switch1, "switch.on", switchOnOneHandler)
    subscribe(switch2, "switch.on", switchOnTwoHandler)
    subscribe(switch3, "switch.on", switchOnThreeHandler)
    subscribe(switch4, "switch.on", switchOnFourHandler)
    subscribe(switch1, "switch.off", switchOffOneHandler)
    subscribe(switch2, "switch.off", switchOffTwoHandler)
    subscribe(switch3, "switch.off", switchOffThreeHandler)
    subscribe(switch4, "switch.off", switchOffFourHandler)
}

def switchOnOneHandler(evt) {
	def data = [:]  // instrumented code
	def cnt = 0 // instrumented code
	log.debug "switch on1"
	cnt++ // instrumented code
	data['action'+cnt] = "sink1: strip on1"  // instrumented code
	strip.on1()
	sendRequest(data)  // instrumented code
}

def switchOnTwoHandler(evt) {
	def data = [:]  // instrumented code
	def cnt = 0 // instrumented code
	log.debug "switch on2"
	cnt++ // instrumented code
	data['action'+cnt] = "sink2: strip on2"  // instrumented code
	strip.on2()
	sendRequest(data)  // instrumented code
}

def switchOnThreeHandler(evt) {
	def data = [:]  // instrumented code
	def cnt = 0 // instrumented code
	log.debug "switch on3"
	cnt++ // instrumented code
	data['action'+cnt] = "sink3: strip on3"  // instrumented code
	strip.on3()
	sendRequest(data)  // instrumented code
}

def switchOnFourHandler(evt) {
	def data = [:]  // instrumented code
	def cnt = 0 // instrumented code
	log.debug "switch on4"
	cnt++ // instrumented code
	data['action'+cnt] = "sink4: strip on4"  // instrumented code
	strip.on4()
	sendRequest(data)  // instrumented code
}

def switchOffOneHandler(evt) {
	def data = [:]  // instrumented code
	def cnt = 0 // instrumented code
	log.debug "switch off1"
	cnt++ // instrumented code
	data['action'+cnt] = "sink5: strip off1"  // instrumented code
	strip.off1()
	sendRequest(data)  // instrumented code
}

def switchOffTwoHandler(evt) {
	def data = [:]  // instrumented code
	def cnt = 0 // instrumented code
	log.debug "switch off2"
	cnt++ // instrumented code
	data['action'+cnt] = "sink6: strip off2"  // instrumented code
	strip.off2()
	sendRequest(data)  // instrumented code
}

def switchOffThreeHandler(evt) {
	def data = [:]  // instrumented code
	def cnt = 0 // instrumented code
	log.debug "switch off3"
	cnt++ // instrumented code
	data['action'+cnt] = "sink7: strip off3"  // instrumented code
	strip.off3()
	sendRequest(data)  // instrumented code
}

def switchOffFourHandler(evt) {
	def data = [:]  // instrumented code
	def cnt = 0 // instrumented code
	log.debug "switch off4"
	cnt++ // instrumented code
	data['action'+cnt] = "sink8: strip off4"  // instrumented code
	strip.off4()
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