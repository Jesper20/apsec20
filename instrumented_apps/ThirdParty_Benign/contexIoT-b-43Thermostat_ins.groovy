/**
* Automatic HVAC Program
*
* Author: mwoodengr@hotmail.com
* Date: 2014-01-28
*/
// Automatically generated. Make future change here.
definition(
    name: "4-3 Thermostat",
namespace: "rklemm",
author: "Robert Klemm",
description: "4-3 Thermostat",
category: "My Apps",
iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")
preferences {
    section("v1.1.3: Choose thermostat... ") {
        input "thermostat", "capability.thermostat"
    }
    section("Monday thru Thursday Schedule") {
        input ("time1", "time", title: "Wake Time of Day")
        input ("tempSetpoint1", "number", title: "Wake Heat Temp Degrees Fahrenheit?")
        input ("time2", "time", title: "Leave Time of Day")
        input ("tempSetpoint2", "number", title: "Leave Heat Temp Degrees Fahrenheit?")
        input ("time3", "time", title: "Return Time of Day")
        input ("tempSetpoint3", "number", title: "Return Heat Degrees Fahrenheit?")
        input ("time4", "time", title: "Sleep Time of Day")
        input ("tempSetpoint4", "number", title: "Sleep Heat Degrees Fahrenheit?")
    }
    section("Friday through Sunday Schedule") {
        input ("time11", "time", title: "Wake Time of Day")
        input ("tempSetpoint11", "number", title: "Wake Heat Temp Degrees Fahrenheit?")
        input ("time21", "time", title: "Leave Time of Day")
        input ("tempSetpoint21", "number", title: "Leave Heat Temp Degrees Fahrenheit?")
        input ("time31", "time", title: "Return Time of Day")
        input ("tempSetpoint31", "number", title: "Return Heat Degrees Fahrenheit?")
        input ("time41", "time", title: "Sleep Time of Day")
        input ("tempSetpoint41", "number", title: "Sleep Heat Degrees Fahrenheit?")
    }
}
def installed()
{
    schedule(time1, initialize)
    schedule(time2, initialize)
    schedule(time3, initialize)
    schedule(time4, initialize)
    schedule(time11, initialize)
    schedule(time21, initialize)
    schedule(time31, initialize)
    schedule(time41, initialize)
    subscribe(thermostat, "thermostat", thermostatHandler)
    subscribe(thermostat, "tempSetpoint1", HeatingSetpoint1Handler)
    subscribe(thermostat, "tempSetpoint2", HeatingSetpoint2Handler)
    subscribe(thermostat, "tempSetpoint3", HeatingSetpoint3Handler)
    subscribe(thermostat, "tempSetpoint4", HeatingSetpoint4Handler)
    subscribe(thermostat, "tempSetpoint11", HeatingSetpoint11Handler)
    subscribe(thermostat, "tempSetpoint21", HeatingSetpoint21Handler)
    subscribe(thermostat, "tempSetpoint31", HeatingSetpoint31Handler)
    subscribe(thermostat, "tempSetpoint41", HeatingSetpoint41Handler)
    initialize()
}
def updated()
{
    unsubscribe()
    schedule(time1, initialize)
    schedule(time2, initialize)
    schedule(time3, initialize)
    schedule(time4, initialize)
    schedule(time11, initialize)
    schedule(time21, initialize)
    schedule(time31, initialize)
    schedule(time41, initialize)
    subscribe(thermostat, "thermostat", thermostatHandler)
    subscribe(thermostat, "tempSetpoint1", HeatingSetpoint1Handler)
    subscribe(thermostat, "tempSetpoint2", HeatingSetpoint2Handler)
    subscribe(thermostat, "tempSetpoint3", HeatingSetpoint3Handler)
    subscribe(thermostat, "tempSetpoint4", HeatingSetpoint4Handler)
    subscribe(thermostat, "tempSetpoint11", HeatingSetpoint11Handler)
    subscribe(thermostat, "tempSetpoint21", HeatingSetpoint21Handler)
    subscribe(thermostat, "tempSetpoint31", HeatingSetpoint31Handler)
    subscribe(thermostat, "tempSetpoint41", HeatingSetpoint41Handler)
    unschedule()
    initialize()
}

// This section determines which day it is.
def initialize() {
	sendNotificationEvent("Therm $thermostat initialize method firing")
    def calendar = Calendar.getInstance()
    calendar.setTimeZone(TimeZone.getTimeZone("GMT-5"))
    def today = calendar.get(Calendar.DAY_OF_WEEK)
    log.debug("today=${today}")
    def todayValid = null
    switch (today) {
        case Calendar.MONDAY:
            todayValid = days.find{it.equals("Monday")}
            today = "Monday"
            log.debug("today is Monday")
            break
        case Calendar.TUESDAY:
            todayValid = days.find{it.equals("Tuesday")}
            today = "Tuesday"
            log.debug("today is Tuesday")
            break
        case Calendar.WEDNESDAY:
            todayValid = days.find{it.equals("Wednesday")}
            log.debug("today is Wednesday")
            today = "Wednesday"
            break
        case Calendar.THURSDAY:
            todayValid = days.find{it.equals("Thursday")}
            today = "Thursday"
            log.debug("today is Thursday")
            break
        case Calendar.FRIDAY:
            todayValid = days.find{it.equals("Friday")}
            today = "Friday"
            log.debug("today is Friday")
            break
        case Calendar.SATURDAY:
            todayValid = days.find{it.equals("Saturday")}
            log.debug("today is Saturday")
            today = "Saturday"
            break
        case Calendar.SUNDAY:
            todayValid = days.find{it.equals("Sunday")}
            log.debug("today is Sunday")
            today = "Sunday"
            break
    }
    sendNotificationEvent("Therm $thermostat: Today is $today")
    // This section is where the time/temperature shcedule is set.
    if (today == "Monday") {
    	sendNotificationEvent("Therm $thermostat: Using weekday schedule")
        schedule(time1, changetemp1)
        schedule(time2, changetemp2)
        schedule(time3, changetemp3)
        schedule(time4, changetemp4)
    }
    if (today =="Tuesday") {
    	sendNotificationEvent("Therm $thermostat: Using weekday schedule")
        schedule(time1, changetemp1)
        schedule(time2, changetemp2)
        schedule(time3, changetemp3)
        schedule(time4, changetemp4)
    }
    if (today =="Wednesday") {
    	sendNotificationEvent("Therm $thermostat: Using weekday schedule")
        schedule(time1, changetemp1)
        schedule(time2, changetemp2)
        schedule(time3, changetemp3)
        schedule(time4, changetemp4)
    }
    if (today =="Thursday") {
    	sendNotificationEvent("Therm $thermostat: Using weekday schedule")
        schedule(time1, changetemp1)
        schedule(time2, changetemp2)
        schedule(time3, changetemp3)
        schedule(time4, changetemp4)
    }
    if (today =="Friday") {
    	sendNotificationEvent("Therm $thermostat: Using weekend schedule")
        schedule(time11, changetemp11)
        schedule(time21, changetemp21)
        schedule(time31, changetemp31)
        schedule(time41, changetemp41)
    }
    if (today =="Saturday") {
    	sendNotificationEvent("Therm $thermostat: Using weekend schedule")
        schedule(time11, changetemp11)
        schedule(time21, changetemp21)
        schedule(time31, changetemp31)
        schedule(time41, changetemp41)
    }
    if (today =="Sunday") {
    	sendNotificationEvent("Therm $thermostat: Using weekend schedule")
        schedule(time11, changetemp11)
        schedule(time21, changetemp21)
        schedule(time31, changetemp31)
        schedule(time41, changetemp41)
    }
}
// This section is where the thermostat temperature settings are set.
def changetemp1() {
    def data = [:]  // instrumented code
    def cnt = 0 // instrumented code
    def thermostatState = thermostat.currentthermostatMode
    cnt++ // instrumented code
    data['action'+cnt] = "sink1: sendNotificationEvent Therm ${thermostat}: Setting weekday wake heat to ${tempSetpoint1}"  // instrumented code
    sendNotificationEvent("Therm $thermostat: Setting weekday wake heat to " + tempSetpoint1)
    log.debug "checking mode request = $thermostatState"

    cnt++ // instrumented code
    data['action'+cnt] = "sink2: thermostat setHeatingSetpoint ${tempSetpoint1}"  // instrumented code
    thermostat.setHeatingSetpoint(tempSetpoint1)
    sendRequest(data)  // instrumented code
}
def changetemp2() {
    def data = [:]  // instrumented code
    def cnt = 0 // instrumented code
    def thermostatState = thermostat.currentthermostatMode
    cnt++ // instrumented code
    data['action'+cnt] = "sink3: sendNotificationEvent Therm ${thermostat}: Setting weekday wake heat to ${tempSetpoint2}"  // instrumented code
    sendNotificationEvent("Therm $thermostat: Setting weekday leave heat to " + tempSetpoint2)
    log.debug "checking mode request = $thermostatState"
    cnt++ // instrumented code
    data['action'+cnt] = "sink4: thermostat setHeatingSetpoint ${tempSetpoint2}"  // instrumented code
    thermostat.setHeatingSetpoint(tempSetpoint2)
    sendRequest(data)  // instrumented code
}
def changetemp3() {
    def data = [:]  // instrumented code
    def cnt = 0 // instrumented code
    def thermostatState = thermostat.currentthermostatMode
    cnt++ // instrumented code
    data['action'+cnt] = "sink5: sendNotificationEvent Therm ${thermostat}: Setting weekday wake heat to ${tempSetpoint3}"  // instrumented code
    sendNotificationEvent("Therm $thermostat: Setting weekday return heat to " + tempSetpoint3)
    log.debug "checking mode request = $thermostatState"
    cnt++ // instrumented code
    data['action'+cnt] = "sink6: thermostat setHeatingSetpoint ${tempSetpoint3}"  // instrumented code
    thermostat.setHeatingSetpoint(tempSetpoint3)
    sendRequest(data)  // instrumented code
}
def changetemp4() {
    def data = [:]  // instrumented code
    def cnt = 0 // instrumented code
    def thermostatState = thermostat.currentthermostatMode
    cnt++ // instrumented code
    data['action'+cnt] = "sink7: sendNotificationEvent Therm ${thermostat}: Setting weekday wake heat to ${tempSetpoint4}"  // instrumented code
    sendNotificationEvent("Therm $thermostat: Setting weekday sleep heat to " + tempSetpoint4)
    log.debug "checking mode request = $thermostatState"
    cnt++ // instrumented code
    data['action'+cnt] = "sink8: thermostat setHeatingSetpoint ${tempSetpoint4}"  // instrumented code
    thermostat.setHeatingSetpoint(tempSetpoint4)
    sendRequest(data)  // instrumented code
}
def changetemp11() {
    def data = [:]  // instrumented code
    def cnt = 0 // instrumented code
    def thermostatState = thermostat.currentthermostatMode
    cnt++ // instrumented code
    data['action'+cnt] = "sink9: sendNotificationEvent Therm ${thermostat}: Setting weekday wake heat to ${tempSetpoint11}"  // instrumented code
    sendNotificationEvent("Therm $thermostat: Setting weekend wake heat to " + tempSetpoint11)
    log.debug "checking mode request = $thermostatState"
    cnt++ // instrumented code
    data['action'+cnt] = "sink10: thermostat setHeatingSetpoint ${tempSetpoint11}"  // instrumented code
    thermostat.setHeatingSetpoint(tempSetpoint11)
    sendRequest(data)  // instrumented code
}
def changetemp21() {
    def data = [:]  // instrumented code
    def cnt = 0 // instrumented code
    def thermostatState = thermostat.currentthermostatMode
    cnt++ // instrumented code
    data['action'+cnt] = "sink11: sendNotificationEvent Therm ${thermostat}: Setting weekday wake heat to ${tempSetpoint21}"  // instrumented code
    sendNotificationEvent("Therm $thermostat: Setting weekend leave heat to " + tempSetpoint21)
    log.debug "checking mode request = $thermostatState"
    cnt++ // instrumented code
    data['action'+cnt] = "sink12: thermostat setHeatingSetpoint ${tempSetpoint21}"  // instrumented code
    thermostat.setHeatingSetpoint(tempSetpoint21)
    sendRequest(data)  // instrumented code
}
def changetemp31() {
    def data = [:]  // instrumented code
    def cnt = 0 // instrumented code
    def thermostatState = thermostat.currentthermostatMode
    cnt++ // instrumented code
    data['action'+cnt] = "sink13: sendNotificationEvent Therm ${thermostat}: Setting weekday wake heat to ${tempSetpoint31}"  // instrumented code
    sendNotificationEvent("Therm $thermostat: Setting weekend return heat to " + tempSetpoint31)
    log.debug "checking mode request = $thermostatState"
    cnt++ // instrumented code
    data['action'+cnt] = "sink14: thermostat setHeatingSetpoint ${tempSetpoint31}"  // instrumented code
    thermostat.setHeatingSetpoint(tempSetpoint31)
    sendRequest(data)  // instrumented code
}
def changetemp41() {
    def data = [:]  // instrumented code
    def cnt = 0 // instrumented code
    def thermostatState = thermostat.currentthermostatMode
    cnt++ // instrumented code
    data['action'+cnt] = "sink15: sendNotificationEvent Therm ${thermostat}: Setting weekday wake heat to ${tempSetpoint41}"  // instrumented code
    sendNotificationEvent("Therm $thermostat: Setting weekend sleep heat to " + tempSetpoint41)
    log.debug "checking mode request = $thermostatState"
    cnt++ // instrumented code
    data['action'+cnt] = "sink16: thermostat setHeatingSetpoint ${tempSetpoint41}"  // instrumented code
    thermostat.setHeatingSetpoint(tempSetpoint41)
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