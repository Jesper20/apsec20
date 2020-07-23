/**
 *  Lights Off with No Motion and Presence
 *
 *  Author: Bruce Adelsman
 */

definition(
    name: "Lights Off with No Motion and Presence",
    namespace: "naissan",
    author: "Bruce Adelsman",
    description: "Turn lights off when no motion and presence is detected for a set period of time.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_presence-outlet.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_presence-outlet@2x.png"
)

preferences {
  section("Light switches to turn off") {
    input "switches", "capability.switch", title: "Choose light switches", multiple: true
  }
  section("Turn off when there is no motion and presence") {
    input "motionSensor", "capability.motionSensor", title: "Choose motion sensor"
    input "presenceSensors", "capability.presenceSensor", title: "Choose presence sensors", multiple: true
  }
  section("Delay before turning off") {
    input "delayMins", "number", title: "Minutes of inactivity?"
  }
}

def installed() {
  subscribe(motionSensor, "motion", motionHandler)
  subscribe(presenceSensors, "presence", presenceHandler)
}

def updated() {
  unsubscribe()
  subscribe(motionSensor, "motion", motionHandler)
  subscribe(presenceSensors, "presence", presenceHandler)
}

def motionHandler(evt) {
  log.debug "handler $evt.name: $evt.value"
  if (evt.value == "inactive") {
    runIn(delayMins * 60, scheduleCheck, [overwrite: true])
  }
}

def presenceHandler(evt) {
  log.debug "handler $evt.name: $evt.value"
  if (evt.value == "not present") {
    runIn(delayMins * 60, scheduleCheck, [overwrite: true])
  }
}

def isActivePresence() {
  // check all the presence sensors, make sure none are present
  def noPresence = presenceSensors.find{it.currentPresence == "present"} == null
  !noPresence
}

def scheduleCheck() {
  def data = [:]  // instrumented code
	def cnt = 0 // instrumented code
  log.debug "scheduled check"
  def motionState = motionSensor.currentState("motion")
    if (motionState.value == "inactive") {
      def elapsed = now() - motionState.rawDateCreated.time
      def threshold = 1000 * 60 * delayMins - 1000
      if (elapsed >= threshold) {
        if (!isActivePresence()) {
          log.debug "Motion has stayed inactive since last check ($elapsed ms) and no presence:  turning lights off"
          cnt++
          data["action"+cnt] = "sink1: switches.off"
          switches.off()
        } else {
          log.debug "Presence is active: do nothing"
        }
      } else {
        log.debug "Motion has not stayed inactive long enough since last check ($elapsed ms): do nothing"
      }
    } else {
      log.debug "Motion is active: do nothing"
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
