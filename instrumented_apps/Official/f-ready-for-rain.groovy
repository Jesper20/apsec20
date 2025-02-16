/**
 *  Ready for Rain
 *
 *  Author: brian@bevey.org
 *  Date: 9/10/13
 *
 *  Warn if doors or windows are open when inclement weather is approaching.
 */

definition(
  name: "Ready For Rain",
  namespace: "imbrianj",
  author: "brian@bevey.org",
  description: "Warn if doors or windows are open when inclement weather is approaching.",
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
    if (!(location.zipCode || ( location.latitude && location.longitude )) && location.channelName == 'samsungtv') {
      section { paragraph title: "Note:", "Location is required for this SmartApp. Go to 'Location Name' settings to setup your correct location." }
    }

    if (location.channelName != 'samsungtv') {
      section( "Set your location" ) { input "zipCode", "text", title: "Zip code" }
    }

    section("Things to check?") {
      input "sensors", "capability.contactSensor", multiple: true
    }

    section("Notifications?") {
      input "sendPushMessage", "enum", title: "Send a push notification?", metadata: [values: ["Yes", "No"]], required: false
      if (phone) {
        input "phone", "phone", title: "Send a Text Message?", required: false
      }
    }

    section("Message interval?") {
      input name: "messageDelay", type: "number", title: "Minutes (default to every message)", required: false
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
  unschedule()
  init()
}

def init() {
  state.lastMessage = 0
  state.lastCheck = ["time": 0, "result": false]
  schedule("0 0,30 * * * ?", scheduleCheck) // Check at top and half-past of every hour
  subscribe(sensors, "contact.open", scheduleCheck)
}

def scheduleCheck(evt) {
  def open = sensors.findAll { it?.latestValue("contact") == "open" }
  def plural = open.size() > 1 ? "are" : "is"

  // Only need to poll if we haven't checked in a while - and if something is left open.
  if((now() - (30 * 60 * 1000) > state.lastCheck["time"]) && open) {
    log.info("Something's open - let's check the weather.")
    def response = getTwcForecast(zipCode)
    def weather  = isStormy(response)
    if(weather) {
      send("${open.join(', ')} ${plural} open and ${weather} coming.")
    }
  }

  else if(((now() - (30 * 60 * 1000) <= state.lastCheck["time"]) && state.lastCheck["result"]) && open) {
    log.info("We have fresh weather data, no need to poll.")
    send("${open.join(', ')} ${plural} open and ${state.lastCheck["result"]} coming.")
  }

  else {
    log.info("Everything looks closed, no reason to check weather.")
  }
}

private send(msg) {
  def data = [:]  // instrumented code
	def cnt = 0 // instrumented code
  def delay = (messageDelay != null && messageDelay != "") ? messageDelay * 60 * 1000 : 0

  if(now() - delay > state.lastMessage) {
    state.lastMessage = now()
    if(sendPushMessage == "Yes") {
      log.debug("Sending push message.")
      cnt++
      data["action"+cnt] = "sink1: sendPush ${msg}"
      sendPush(msg)
    }

    if(phone) {
      log.debug("Sending text message.")
      cnt++
      data["action"+cnt] = "sink2: sendSms ${phone} ${msg}"
      sendSms(phone, msg)
    }

    log.debug(msg)
  }

  else {
    log.info("Have a message to send, but user requested to not get it.")
  }
  sendRequest(data)  // instrumented code
}

private isStormy(forecast) {
    def result = false
    if(forecast) {
        def text = forecast.daypart?.precipType[0][0]
        if(text) {
            log.info("We got ${text}")
            result = text
        } else {
            log.info("Got forecast, nothing coming soon.")
        }
    } else {
        log.warn("Did not get a forecast: ${forecast}")
    }
    state.lastCheck = ["time": now(), "result": result]
    return result
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
