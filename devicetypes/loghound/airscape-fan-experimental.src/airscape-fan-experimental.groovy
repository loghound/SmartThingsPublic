/**
 *  Copyright 2015 Charles Schwer
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 * 	Airscape Fan (Gen 2 controls)
 *
 * 	Author: loghound
 * 	Date: May 1, 2018
 */
preferences {
    input("ip", "string", title: "IP Address", description: "10.0.0.236", defaultValue: "10.0.0.236", required: true, displayDuringSetup: true)
    input("port", "string", title: "Port", description: "80", defaultValue: 80, required: true, displayDuringSetup: true)

}

/* Stringify needs switch, polling, configuraiton, switch level, refresh, actuator */

metadata {
    definition(name: "airscape fan experimental", namespace: "loghound", author: "loghound") {
        capability "Polling"
        capability "Refresh"
        capability "Sensor"
        capability "Actuator"
        capability "Configuration"

        capability "Temperature Measurement"
        capability "Switch"
        capability "Switch Level"

    }

    // simulator metadata
    simulator {}

    // UI tile definitions
    tiles() {
        valueTile("temperature", "device.temperature", width: 1, height: 1) {
            state "temperature", label: '${currentValue}°F', unit: "",
                    backgroundColors: [
                            [value: 31, color: "#153591"],
                            [value: 44, color: "#1e9cbb"],
                            [value: 59, color: "#90d2a7"],
                            [value: 74, color: "#44b621"],
                            [value: 84, color: "#f1d801"],
                            [value: 95, color: "#d04e00"],
                            [value: 96, color: "#bc2323"]
                    ]
        }

        valueTile("temperatureoa", "device.temperatureoa", width: 1, height: 1) {
            state "temperature", label: 'OA${currentValue}°F', unit: "",
                    backgroundColors: [
                            [value: 31, color: "#153591"],
                            [value: 44, color: "#1e9cbb"],
                            [value: 59, color: "#90d2a7"],
                            [value: 74, color: "#44b621"],
                            [value: 84, color: "#f1d801"],
                            [value: 95, color: "#d04e00"],
                            [value: 96, color: "#bc2323"]
                    ]
        }

        valueTile("temperatureat", "device.temperatureattic", width: 1, height: 1) {
            state "temperature", label: 'Attic ${currentValue}°F', unit: "",
                    backgroundColors: [
                            [value: 31, color: "#153591"],
                            [value: 44, color: "#1e9cbb"],
                            [value: 59, color: "#90d2a7"],
                            [value: 74, color: "#44b621"],
                            [value: 84, color: "#f1d801"],
                            [value: 95, color: "#d04e00"],
                            [value: 96, color: "#bc2323"]
                    ]
        }

        standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true) {
            state "off", label: '${currentValue}', action: "switch.on",
                    icon: "st.switches.switch.off", backgroundColor: "#ffffff"
            state "on", label: '${currentValue}', action: "switch.off",
                    icon: "st.switches.switch.on", backgroundColor: "#79b821"
        }

        controlTile("level", "device.level", "slider",
                height: 2, width: 2) {
            state "level", action: "switch level.setLevel"
        }

        valueTile("power", "device.power") {
            // label will be the current value of the power attribute
            state "power", label: '${currentValue} W'
        }


        standardTile("refresh", "device.backdoor", inactiveLabel: false, decoration: "flat") {
            state "default", label: '', action: "refresh.refresh", icon: "st.secondary.refresh"
        }

        main "temperature"
        details(["temperature", "temperatureoa", "temperatureat", "switch", "power", "level", "refresh"])
    }
}

def configure() {
    log.debug "************ In Configure ***********"
}

/* on() and off() are used as part of binary switches */
def on() {
    log.debug "Switch On"
    def result = setDir(1)
    result
    return result
}

def off() {
    log.debug "Switch Off"
    def result = setDir(4)
    result
    return result
}

/* Level is used as part of a multilevel switch */
def level(level, rate) {
    log.debug "level is ${level}"
}

def setLevel(level) {

	log.debug "*** set level to ${level} device is at ${state.level}"
    
    Integer speed = level / 100.0 * 7

	def delta=speed-state.level
    if (delta.abs()<3) {
    	return // not big enough
    }
    
    def result

    log.debug "setLevel is ${level} and speed should be ${speed}"

    if (speed == 0) {
        result = setDir(4)
        log.debug "Set fan to off ${result}"
    } else if (level-state.level > 0) {
        result = setDir(1)
        log.debug "Set fan to one faster speed ${result}"

    } else if (level-state.level <0) {
        result = setDir(3)
        log.debug "Set fan down a speed ${result}"
    }
    log.debug result
    result
    
    state.level=level
    
    return result
}

def setDir(dir) {
/** 
	see this post for descriptoin of the fanspeed.cgi scrip
    http://blog.airscapefans.com/archives/gen-2-controls-api
    
    in short 
    If you want to get data from the controller without any control actions, you can send the same HTTP command string without the “?dir=|1|2|3|4|” suffix.

   For example, if your fan is at IP 192.168.0.20 the command would be:

   http://192.168.0.20/fanspd.cgi
   http://controllerURL/fanspd.cgi?dir=|1|2|3|4|
   where 1=fan speed up, 2=timer hour add, 3=fan speed down, 4=fan off

   For XML and JSON formatted responses use the following commands with your fan IP inserted:

   http://192.168.0.20/status.xml.cgi  gives data in xml format
   http://192.168.0.20/status.json.cgi  gives data in json format



**/

    log.debug "Polling"

 
    def port = 80
    def path
    if (dir == 0)
        path = "/fanspd.cgi"
    else
        path = "/fanspd.cgi?dir=${dir}"

    log.debug "Using ip: ${ip} and port: ${port} for device: ${device.id} and path ${path}"
    def result = new physicalgraph.device.HubAction(
            method: "GET",
            path: path,
            headers: [
                    HOST: "${ip}:${port}"
            ]
    )
    
    	return result
}



def initialize() {
    log.debug "starting a run every 5 minute"
    runEvery5Minutes(poll)
}

// parse events into attributes
def parse(String description) {
    def result = []
    def txt = parseLanMessage(description).body
    log.debug txt
    def oa_temp = txt =~ ~/oa_temp>(.+?)</
    def house_temp = txt =~ ~/house_temp>(.+?)</
    def attic_temp = txt =~ ~/attic_temp>(.+?)</
    def power = txt =~ ~/power>(.+?)</
    def fanspd = txt =~ ~/fanspd>(.+?)</

    def doorStatus = txt =~ ~/doorinprocess>(.+?)</

    if (house_temp) {
        state.temperature=house_temp[0][1]
        result << createEvent(name: "temperature", value: state.temperature, displayed: true)
        log.debug "Setting temperature to $house_temp[0][1]"
    }

    if (oa_temp) {
    	state.temperatureoa=oa_temp[0][1]
        result << createEvent(name: "temperatureoa", value: state.temperatureoa, displayed: true)
        log.debug "Setting temperature to $oa_temp[0][1]"
    }

    if (attic_temp) {
    	state.temperatureattic=attic_temp[0][1]
        result << createEvent(name: "temperatureattic", value: state.temperatureattic, displayed: true)
        log.debug "Setting temperature to $attic_temp[0][1]"
    }

    if (power) {
        def powerDisplay = power[0][1].toInteger()
        if (powerDisplay == 0) {
            log.debug "Door Status is ${doorStatus}"
            def doorInProgress = doorStatus[0][1].toInteger()
            if (doorInProcess > 0)
                powerDisplay = 10 // Reasonable guess for now -- just toss up 10W
        }
		state.power=powerDisplay
        result << createEvent(name: "power", value: powerDisplay.toString(), displayed: true)
    }

    // Emulate a binary switch
    if (fanspd) {
        def spdInt = fanspd[0][1].toInteger()
        if (spdInt == 0) {
            result << createEvent(name: "switch", value: "off", displayed: true)
            result << createEvent(name: "level", value: 0, displayed: true)
            state.level=0
        } else {
            result << createEvent(name: "switch", value: "on", displayed: true)
            result << createEvent(name: "level", value: spdInt * 100.0 / 7.0, displayed: true)
            state.level=spdInt*100.0/7.0
            
        }

    }

    // result << createEvent(name: "temperature", value: new Random().nextInt(100) + 1, displayed: true )

    log.debug "Parsing '${description}'"
    def msg = parseLanMessage(description)
    def headerString = msg.header

    if (!headerString) {
        log.debug "headerstring was null for some reason :("
    }


    def bodyString = msg.body
    def value = "";
    if (bodyString) {
        log.debug bodyString
        // default the contact and motion status to closed and inactive by default
        def allContactStatus = "closed"
        def allMotionStatus = "inactive"
        def json = msg.json;
        json?.house?.door?.each { door ->
            value = door?.status == 1 ? "open" : "closed"
            log.debug "${door.name} door status ${value}"
            // if any door is open, set contact to open
            if (value == "open") {
                allContactStatus = "open"
            }
            result << creatEvent(name: "temperature", value: allContactStatus)
        }

        //result << createEvent(name: "motion", value: allMotionStatus)
    }

    log.debug result
    result
}


def refresh() {
    log.debug "refresh"


    poll()
}

def poll() {

    def result = setDir(0)
    return result
}

