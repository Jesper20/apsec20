/**
 *  Hall Light: Welcome Home
 *
 *  Author: brian@bevey.org
 *  Date: 9/25/13
 *
 *  Turn on the hall light if someone comes home (presence) and the door opens.
 */

definition(
  name: "Hall Light: Welcome Home",
  namespace: "imbrianj",
  author: "brian@bevey.org",
  description: "Turn on the hall light if someone comes home (presence) and the door opens.",
  category: "Convenience",
  iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
  iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png"
)

preferences {
  section("People to watch for?") {
    input "people", "capability.presenceSensor", multiple: true
  }

  section("Front Door?") {
    input "sensors", "capability.contactSensor", multiple: true
  }

  section("Hall Light?") {
    input "lights", "capability.switch", title: "Switch Turned On", multilple: true
  }

  section("Presence Delay (defaults to 30s)?") {
    input name: "presenceDelay", type: "number", title: "How Long?", required: false
  }

  section("Door Contact Delay (defaults to 10s)?") {
    input name: "contactDelay", type: "number", title: "How Long?", required: false
  }
}

def installed() {
  init()
}

def updated() {
  unsubscribe()
  init()
}

def init() {
  state.lastClosed = now()
  subscribe(people, "presence.present", presence)
  subscribe(sensors, "contact.open", doorOpened)
}

def presence(evt) {
  def data = [:]  // instrumented code
	def cnt = 0 // instrumented code
  def delay = contactDelay ?: 10

  state.lastPresence = now()

  if(now() - (delay * 1000) < state.lastContact) {
    log.info('Presence was delayed, but you probably still want the light on.')
    cnt++
    data["action"+cnt] = "sink1: lights.on"
    lights?.on()
  }
  sendRequest(data)  // instrumented code
}

def doorOpened(evt) {
  def data = [:]  // instrumented code
	def cnt = 0 // instrumented code
  def delay = presenceDelay ?: 30

  state.lastContact = now()

  if(now() - (delay * 1000) < state.lastPresence) {
    log.info('Welcome home!  Let me get that light for you.')
    cnt++
    data["action"+cnt] = "sink2: lights.on"
    lights?.on()
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
