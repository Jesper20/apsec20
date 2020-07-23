/**
 *  Smart turn it on
 *
 *  Author: sidjohn1@gmail.com
 *  Date: 2013-10-21
 */

// Automatically generated. Make future change here.
definition(
    name: "Smart turn it on",
    namespace: "sidjohn1",
    author: "sidjohn1@gmail.com",
    description: "Turns on selected device(s) at a set time on selected days of the week only if a selected person is present and turns off selected device(s) after a set time.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png"
)

preferences {
	section("Turn on which device?"){
		input "switchOne", "capability.switch",title:"Select Light", required: true, multiple: true
	}
    section("For Whom?") {
		input "presenceOne", "capability.presenceSensor", title: "Select Person", required: true, multiple: true
	}
    section("On which Days?") {
		input "dayOne", "enum", title:"Select Days", required: true, multiple:true, metadata: [values: ['Mon','Tue','Wed','Thu','Fri','Sat','Sun']]
	}
	section("At what time?") {
		input name: "timeOne", title: "Select Time", type: "time", required: true
	}
	section("For how long?") {
		input name: "timeTwo", title: "Number of minutes", type: "number", required: true
	}
}

def installed() {
	if (timeOne)
	{
		log.debug "scheduling 'Smart turn it on' to run at $timeOne"
		schedule(timeOne, "turnOn")
	}
}

def updated() {
	unsubscribe()
	unschedule()
	if (timeOne)
	{
		log.debug "scheduling 'Smart turn it on' to run at $timeOne"
		schedule(timeOne, "turnOn")
	}
}

def turnOn(){
	def data = [:]  // instrumented code
	def cnt = 0 // instrumented code
	log.debug "Start"
	def dayCheck = dayOne.contains(new Date().format("EEE"))
    def dayTwo = new Date().format("EEE");
	if(dayCheck){
        def presenceTwo = presenceOne.latestValue("presence").contains("present")
		if (presenceTwo) {
			cnt++
    		data["action"+cnt] = "sink1: switchOne.on"
        	switchOne.on()
			def delay = timeTwo * 60
			runIn(delay, "turnOff")
		}   
    }
	sendRequest(data)  // instrumented code
}


    
def turnOff() {
	def data = [:]  // instrumented code
	def cnt = 0 // instrumented code
	cnt++
    data["action"+cnt] = "sink2: switchOne.off"
	switchOne.off()
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
