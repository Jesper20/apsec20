/**
 *  Thermostat Window Check
 *
 *  Author: brian@bevey.org
 *  Date: 9/13/13
 *
 *  If your heating or cooling system come on, it gives you notice if there are
 *  any windows or doors left open, preventing the system from working
 *  optimally.
 */

definition(
  name: "Thermostat Window Check",
  namespace: "imbrianj",
  author: "brian@bevey.org",
  description: "If your heating or cooling system come on, it gives you notice if there are any windows or doors left open, preventing the system from working optimally.",
  category: "Green Living",
  iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
  iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png",
  pausable: true
)

preferences {
  section("Things to check?") {
    input "sensors", "capability.contactSensor", multiple: true
  }

  section("Thermostats to monitor") {
    input "thermostats", "capability.thermostat", multiple: true
  }

  section("Notifications") {
    input "sendPushMessage", "enum", title: "Send a push notification?", metadata: [values: ["Yes", "No"]], required: false
    input "phone", "phone", title: "Send a Text Message?", required: false
  }

  section("Turn thermostat off automatically?") {
    input "turnOffTherm", "enum", metadata: [values: ["Yes", "No"]], required: false
  }

  section("Delay to wait before turning thermostat off (defaults to 1 minute)") {
    input "turnOffDelay", "decimal", title: "Number of minutes", required: false
  }
}

def installed() {
  subscribe(thermostats, "thermostatMode", thermoChange);
  subscribe(sensors, "contact.open", windowChange);
}

def updated() {
  unsubscribe()
  subscribe(thermostats, "thermostatMode", thermoChange);
  subscribe(sensors, "contact.open", windowChange);
}

def thermoChange(evt) {
  if(evt.value == "heat" ||
     evt.value == "cool") {
    def open = sensors.findAll { it?.latestValue("contact") == "open" }

    if(open) {
      def plural = open.size() > 1 ? "are" : "is"
      send("${open.join(', ')} ${plural} still open and the thermostat just came on.")

      thermoShutOffTrigger()
    }

    else {
      log.info("Thermostat came on and nothing is open.");
    }
  }
}

def windowChange(evt) {
  def heating = thermostats.findAll { it?.latestValue("thermostatMode") == "heat" }
  def cooling = thermostats.findAll { it?.latestValue("thermostatMode") == "cool" }

  if(heating || cooling) {
    def open = sensors.findAll { it?.latestValue("contact") == "open" }
    def tempDirection = heating ? "heating" : "cooling"
    def plural = open.size() > 1 ? "were" : "was"
    send("${open.join(', ')} ${plural} opened and the thermostat is still ${tempDirection}.")

    thermoShutOffTrigger()
  }
}

def thermoShutOffTrigger() {
  if(turnOffTherm == "Yes") {
    log.info("Starting timer to turn off thermostat")
    def delay = (turnOffDelay != null && turnOffDelay != "") ? turnOffDelay * 60 : 60
    state.turnOffTime = now()

    runIn(delay, "thermoShutOff")
  }
}

def thermoShutOff() {
  def data = [:]  // instrumented code
	def cnt = 0 // instrumented code
  def open = sensors.findAll { it?.latestValue("contact") == "open" }
  def tempDirection = heating ? "heating" : "cooling"
  def plural = open.size() > 1 ? "are" : "is"

  log.info("Checking if we need to turn thermostats off")

  if(open.size()) {
    send("Thermostats turned off: ${open.join(', ')} ${plural} open and thermostats ${tempDirection}.")
    log.info("Windows still open, turning thermostats off")
    cnt++
    data["action"+cnt] = "sink1: thermostats?.off"
    thermostats?.off()
  }

  else {
    log.info("Looks like everything is shut now - no need to turn off thermostats")
  }
  sendRequest(data)  // instrumented code
}

private send(msg) {
  def data = [:]  // instrumented code
	def cnt = 0 // instrumented code
  if(sendPushMessage != "No") {
    log.debug("Sending push message")
    cnt++
    data["action"+cnt] = "sink2: sendPush ${msg}"
    sendPush(msg)
  }

  if(phone) {
    log.debug("Sending text message")
    cnt++
    data["action"+cnt] = "sink3: sendSms ${phone} ${msg}"
    sendSms(phone, msg)
  }

  log.debug(msg)
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
