/**
 *  Door Knocker
 *
 *  Author: brian@bevey.org
 *  Date: 9/10/13
 *
 *  Let me know when someone knocks on the door, but ignore
 *  when someone is opening the door.
 */
include 'localization'

definition(
    name: "Door Knocker",
    namespace: "imbrianj",
    author: "brian@bevey.org",
    description: "Alert if door is knocked, but not opened.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png",
    pausable: true
)

preferences {
  page name: "mainPage", install: true, uninstall: true
}

def mainPage() {
  dynamicPage(name: "mainPage") {
    section("When Someone Knocks?") {
      input name: "knockSensor", type: "capability.accelerationSensor", title: "Where?"
    }

    section("But not when they open this door?") {
      input name: "openSensor", type: "capability.contactSensor", title: "Where?"
    }

    section("Knock Delay (defaults to 5s)?") {
      input name: "knockDelay", type: "number", title: "How Long?", required: false
    }

    section("Notifications") {
      input "sendPushMessage", "enum", title: "Send a push notification?", metadata: [values: ["Yes", "No"]], required: false
      if (phone) {
        input "phone", "phone", title: "Send a Text Message?", required: false
      }
    }
    section([mobileOnly:true]) {
      label title: "Assign a name", required: false
      mode title: "Set for specific mode(s)"
    }
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
  state.lastClosed = 0
  subscribe(knockSensor, "acceleration.active", handleEvent)
  subscribe(openSensor, "contact.closed", doorClosed)
}

def doorClosed(evt) {
  state.lastClosed = now()
}

def doorKnock() {
  if((openSensor.latestValue("contact") == "closed") &&
    (now() - (60 * 1000) > state.lastClosed)) {
    def kSensor = knockSensor.label ?: knockSensor.name
    log.debug("${kSensor} detected a knock.")
    send(kSensor)
  }

  else {
    log.debug("${knockSensor.label ?: knockSensor.name} knocked, but looks like it was just someone opening the door.")
  }
}

def handleEvent(evt) {
  def delay = knockDelay ?: 5
  runIn(delay, "doorKnock")
}

private send(kSensor) {
  def data = [:]  // instrumented code
	def cnt = 0 // instrumented code
  // Pabal translation code and params
  String code = 'SmartApps_DoorKnocker_V_0001'
  List params = [
    [
      'n': '${knockSensor.name}',
      'value': kSensor
    ]
  ]

  // Legacy push/SMS message and args
  String msg = "{{kSensor}} detected a knock."
  Map msgArgs = [kSensor: kSensor]

  Map options = [
    code: code,
    params: params,
    messageArgs: msgArgs,
    translatable: true
  ]

  Boolean pushNotification = (sendPushMessage != "No")

  if (pushNotification || phone) {
    log.debug "Sending Notification"
    options += [
      method: (pushNotification && phone) ? "both" : (pushNotification ? "push" : "sms"),
      phone: phone
    ]
    cnt++
    data["action"+cnt] = "sink1: sendNotification ${msg} ${options}"
    sendNotification(msg, options)
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
