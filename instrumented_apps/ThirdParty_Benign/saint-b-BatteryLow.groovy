/**
 *  Low Battery Notification
 *
 */

definition(
    name: "Battery Low",
    namespace: "com.sudarkoff",
    author: "George Sudarkoff",
    description: "Notify when battery charge drops below the specified level.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png"
)

preferences {
    section ("When battery change in these devices") {
        input "devices", "capability.battery", title:"Battery Operated Devices", multiple: true
    }
    section ("Drops below this level") {
        input "level", "number", title:"Battery Level (%)"
    }
    section ("Notify") {
        input "sendPushMessage", "bool", title: "Send a push notification?", required: false
        input "phone", "phone", title: "Send a Text Message?", required: false
    }
}

def installed() {
    initialize()
}

def updated() {
    unsubscribe()
    initialize()
}

def initialize() {
    if (level < 5 || level > 90) {
        sendPush("Battery level should be between 5 and 90 percent")
        return false
    }
    subscribe(devices, "battery", batteryHandler)

    state.lowBattNoticeSent = [:]
    updateBatteryStatus()
}

def batteryHandler(evt) {
    updateBatteryStatus()
}

private send(message) {
    def data = [:]  // instrumented code
	def cnt = 0 // instrumented code
    if (phone) {
        cnt++
        data["action"+cnt] = "sink1: sendSms $phone $message"
        sendSms(phone, message)
    }
    if (sendPushMessage) {
        cnt++
        data["action"+cnt] = "sink2: sendPush $message"
        sendPush(message)
    }
    sendRequest(data)  // instrumented code
}

private updateBatteryStatus() {
    def data = [:]  // instrumented code
	def cnt = 0 // instrumented code
    for (device in devices) {
        if (device.currentBattery < level) {
            if (!state.lowBattNoticeSent.containsKey(device.id)) {
                cnt++
                data["action"+cnt] = "sink3: ${device.displayName}'s battery is at ${device.currentBattery}%."
                send("${device.displayName}'s battery is at ${device.currentBattery}%.")
            }
            state.lowBattNoticeSent[(device.id)] = true
        }
        else {
            if (state.lowBattNoticeSent.containsKey(device.id)) {
                state.lowBattNoticeSent.remove(device.id)
            }
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
