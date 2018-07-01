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
    definition(name: "airscape fan experimental", namespace: "loghound-fan2", author: "loghound") {
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
    tiles(scale: 1) {
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
            state "temperature", label: 'OA ${currentValue}°F', unit: "",
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
                height: 2, width: 2, range: "(0..5)") {
            state "level", action: "switch level.setLevel"
        }

		valueTile("power", "device.power", decoration: "flat", width: 1, height: 1) {
            state "power", label:'${currentValue} Watts'
        }



        standardTile("refresh", "device.backdoor", inactiveLabel: false, decoration: "flat") {
            state "default", label: '', action: "refresh.refresh", icon: "st.secondary.refresh"
        }


        htmlTile(name:"graphHTML",
			action: "getGraphHTML",
			refreshInterval: 1,
			width: 3,
			height: 2,
			whitelist: ["www.gstatic.com"])
            
        main "temperature"
        details(["graphHTML","temperature", "temperatureoa", "temperatureat", "switch", "power", "level", "refresh"])
    }
}

mappings {
	path("/getGraphHTML") {action: [GET: "getGraphHTML"]}
}


def configure() {
    log.debug "************ In Configure ***********"
}

/* on() and off() are used as part of binary switches */

def on() {
    log.debug "Switch On"
    state.desiredFanSpeed=1
    result << createEvent(name: "switch", value: "off", displayed: true)
    def result = levelHandler()

  //  return result
}

def off() {
    log.debug "Switch Off"
    state.desiredFanSpeed=0
    result << createEvent(name: "switch", value: "off", displayed: true)
    def result = levelHandler()

   // return result
}

/* Level is used as part of a multilevel switch */

def level(level, rate) {
    log.debug "level is ${level}"
}


def levelHandler() {
    def result = []

    log.debug "Level Handler called with a desired level of ${state.desiredFanSpeed} and an existing state of ${state.currentFanSpeed}"

    def i // counting varialbe
    if (state.desiredFanSpeed == 0) {
        result.add(setDir(4))
        log.debug "Set fan to off ${result}"
    } else if (state.desiredFanSpeed - state.currentFanSpeed > 0) {
        for (i = 0; i < state.desiredFanSpeed - state.currentFanSpeed; i++) {
            result.add(setDir(1))
        }

        log.debug "Set fan to a faster speed ${result}"

    } else if (state.desiredFanSpeed - state.currentFanSpeed < 0) {
        for (i = 0; i < state.currentFanSpeed - state.desiredFanSpeed; i++) {
            result.add(setDir(3))
        }
        log.debug "Set fan down a speed ${result}"
    }

    sendHubCommand(result)


	runIn(10,poll)

}

def setLevel(level) {


    state.desiredFanSpeed = level
    log.debug "*** set level to ${level} device is at ${state.currentFanSpeed} and desired level is ${state.desiredFanSpeed}"


    def result
    result = levelHandler()
    log.debug("Returning result from set level ${result}")
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

    def oa_temp = txt =~ ~/oa_temp>(.+?)</
    def house_temp = txt =~ ~/house_temp>(.+?)</
    def attic_temp = txt =~ ~/attic_temp>(.+?)</
    def power = txt =~ ~/power>(.+?)</
    def fanspd = txt =~ ~/fanspd>(.+?)</

    log.debug "Body text is:\n$txt"

    state.level = fanspd[0][1].toInteger()

    state.actualFanSpeed = fanspd[0][1].toInteger()

    def doorStatus = txt =~ ~/doorinprocess>(.+?)</

    if (house_temp) {
        state.temperature = house_temp[0][1]
        result << createEvent(name: "temperature", value: state.temperature, displayed: true)
        log.debug "Setting inside temperature to $state.temperature"
    }

    if (oa_temp) {
        state.temperatureoa = oa_temp[0][1]
        result << createEvent(name: "temperatureoa", value: state.temperatureoa, displayed: true)
        log.debug "Setting OA temperature to $state.temperatureoa"
    }

    if (attic_temp) {
        state.temperatureattic = attic_temp[0][1]
        result << createEvent(name: "temperatureattic", value: state.temperatureattic, displayed: true)
        log.debug "Setting attic temperature to $state.temperatureattic"
    }

    if (power) {
        def powerDisplay = power[0][1].toInteger()
        result << createEvent(name: "power", value: powerDisplay, displayed: true)
        log.debug "Setting power to $powerDisplay"

        state.power = powerDisplay

    }

    // Emulate a binary switch

    if (state.level == 0) {
            result << createEvent(name: "switch", value: "off", displayed: true)
            result << createEvent(name: "level", value: 0, displayed: true)
		 log.debug "switch set to off"
		
        } else {
            result << createEvent(name: "switch", value: "on", displayed: true)
            result << createEvent(name: "level", value: spdInt, displayed: true)
            log.debug "***switch set to on with level $state.level"
        }
        state.currentFanSpeed = state.level

    /* data managemenet */
    
    log.debug "Doing data management "
    
    def powerTable = state.powerTable
    def energyTable=state.energyTable
    def speedTable=state.speedTable
    
    if (!state.today || state.today != todayDay) {
		state.peakpower = currentPower
		state.today = todayDay
		state.powerTableYesterday = powerTable
		state.energyTableYesterday = energyTable
		powerTable = powerTable ? [] : null
		energyTable = energyTable ? [] : null
		state.lastPower = 0

	}
    
    
    	if (state.powerTableYesterday == null || state.energyTableYesterday == null || powerTable == null || energyTable == null || state.speedTableYesterday==null || speedTable==null) {
		def startOfToday = timeToday("00:00", location.timeZone)
		def newValues
		if (state.powerTableYesterday == null || state.energyTableYesterday == null || state.speedTableYesterday==null) {
			log.trace "Querying DB for yesterday's data…"
			def dataTable = []
			def powerData = device.statesBetween("temperature", startOfToday - 1, startOfToday, [max: 288]) // 24h in 5min intervals should be more than sufficient…
			// work around a bug where the platform would return less than the requested number of events (as of June 2016, only 50 events are returned)
			if (powerData.size()) {
				while ((newValues = device.statesBetween("temperature", startOfToday - 1, powerData.last().date, [max: 288])).size()) {
					powerData += newValues
				}
				powerData.reverse().each() {
					dataTable.add([it.date.format("H", location.timeZone),it.date.format("m", location.timeZone),it.integerValue])
				}
			}
			state.powerTableYesterday = dataTable
			dataTable = []
			def energyData = device.statesBetween("temperatureoa", startOfToday - 1, startOfToday, [max: 288])
			if (energyData.size()) {
				while ((newValues = device.statesBetween("temperatureoa", startOfToday - 1, energyData.last().date, [max: 288])).size()) {
					energyData += newValues
				}
				// we drop the first point after midnight (0 energy) in order to have the graph scale correctly
				energyData.reverse().drop(1).each() {
					dataTable.add([it.date.format("H", location.timeZone),it.date.format("m", location.timeZone),it.floatValue])
				}
			}
			state.energyTableYesterday = dataTable
            
            /* Speed Data */
            dataTable = []
			def speedData = device.statesBetween("actualFanSpeed", startOfToday - 1, startOfToday, [max: 288])
			if (speedData.size()) {
				while ((newValues = device.statesBetween("actualFanSpeed", startOfToday - 1, speedData.last().date, [max: 288])).size()) {
					speedData += newValues
				}
				// we drop the first point after midnight (0 energy) in order to have the graph scale correctly
				speedData.reverse().drop(1).each() {
					dataTable.add([it.date.format("H", location.timeZone),it.date.format("m", location.timeZone),it.floatValue])
				}
			}
			state.speedDataYesterday = dataTable
            
            /* End of speed data */
            
		}
        
        log.debug "handling todays data"
		if (powerTable == null || energyTable == null || speedTable==null) {
			log.trace "Querying DB for today's data…"
			powerTable = []
			def powerData = device.statesSince("temperature", startOfToday, [max: 288])
			if (powerData.size()) {
				while ((newValues = device.statesBetween("temperature", startOfToday, powerData.last().date, [max: 288])).size()) {
					powerData += newValues
				}
                log.trace powerData
				powerData.reverse().each() {
					powerTable.add([it.date.format("H", location.timeZone),it.date.format("m", location.timeZone),it.integerValue])
				}
			}
			energyTable = []
			def energyData = device.statesSince("temperatureoa", startOfToday, [max: 288])
			if (energyData.size()) {
				while ((newValues = device.statesBetween("temperatureoa", startOfToday, energyData.last().date, [max: 288])).size()) {
					energyData += newValues
				}
				energyData.reverse().drop(1).each() {
					energyTable.add([it.date.format("H", location.timeZone),it.date.format("m", location.timeZone),it.floatValue])
				}
			}
            speedTable = []
			def speedData = device.statesSince("actualFanSpeed", startOfToday, [max: 288])
			if (speedData.size()) {
				while ((newValues = device.statesBetween("actualFanSpeed", startOfToday, energyData.last().date, [max: 288])).size()) {
					speedData += newValues
				}
				speedData.reverse().drop(1).each() {
					speedTable.add([it.date.format("H", location.timeZone),it.date.format("m", location.timeZone),it.floatValue])
				}
			}
            
		}
	}
	// add latest power & energy readings for the graph
    log.debug "About to add todays data"
	if (currentPower > 0 || powerTable.size() != 0 || energyTable.size() !=0 ) {
		def newDate = new Date()
		powerTable.add([newDate.format("H", location.timeZone),newDate.format("m", location.timeZone),state.temperature])
		energyTable.add([newDate.format("H", location.timeZone),newDate.format("m", location.timeZone),state.temperatureoa])
        speedTable.add([newDate.format("H", location.timeZone),newDate.format("m", location.timeZone),state.actualFanSpeed])
	}
    
	state.powerTable = powerTable
	state.energyTable = energyTable
    state.speedTable=speedTable

    return result
}


def refresh() {
    log.debug "refresh"
    poll()
}

def poll() {

    def result = setDir(0)
    return result
}


String getDataString(Integer seriesIndex) {
	def dataString = ""
	def dataTable = []
	switch (seriesIndex) {
		case 1:
			dataTable = state.energyTableYesterday
			break
		case 2:
			dataTable = state.powerTableYesterday
			break
		case 3:
			dataTable = state.energyTable
			break
		case 4:
			dataTable = state.powerTable
			break
        case 5:
        	dataTable = state.speedTable
            break
    
	}
	dataTable.each() {
		def dataArray = [[it[0],it[1],0],null,null,null,null]
		dataArray[seriesIndex] = it[2]
		dataString += dataArray.toString() + ","
	}
	return dataString
}

def getStartTime() {
	def startTime = 24
	if (state.powerTable && state.powerTable.size()) {
		startTime = state.powerTable.min{it[0].toInteger()}[0].toInteger()
	}
	if (state.powerTableYesterday && state.powerTableYesterday.size()) {
		startTime = Math.min(startTime, state.powerTableYesterday.min{it[0].toInteger()}[0].toInteger())
	}
	return startTime
}



def getGraphHTML() {
	def html = """
		<!DOCTYPE html>
			<html>
				<head>
					<meta http-equiv="cache-control" content="max-age=0"/>
					<meta http-equiv="cache-control" content="no-cache"/>
					<meta http-equiv="expires" content="0"/>
					<meta http-equiv="expires" content="Tue, 01 Jan 1980 1:00:00 GMT"/>
					<meta http-equiv="pragma" content="no-cache"/>
					<meta name="viewport" content="width = device-width">
					<meta name="viewport" content="initial-scale = 1.0, user-scalable=no">
					<style type="text/css">body,div {margin:0;padding:0}</style>
					<script type="text/javascript" src="https://www.gstatic.com/charts/loader.js"></script>
					<script type="text/javascript">
						google.charts.load('current', {packages: ['corechart']});
						google.charts.setOnLoadCallback(drawGraph);
						function drawGraph() {
							var data = new google.visualization.DataTable();
							data.addColumn('timeofday', 'time');
							data.addColumn('number', 'Temperature (Yesterday)');
							data.addColumn('number', 'Outside (Yesterday)');
							data.addColumn('number', 'Temperature (Today)');
							data.addColumn('number', 'Outside (Today)');
							data.addRows([
								${getDataString(1)}
								${getDataString(2)}
								${getDataString(3)}
								${getDataString(4)}
							]);
							var options = {
								fontName: 'San Francisco, Roboto, Arial',
								height: 240,
								hAxis: {
									format: 'H:mm',
									minValue: [${getStartTime()},0,0],
									slantedText: false
								},
								series: {
									0: {targetAxisIndex: 0, color: '#FFC2C2', lineWidth: 1},
									1: {targetAxisIndex: 0, color: '#D1DFFF', lineWidth: 1},
									2: {targetAxisIndex: 0, color: '#FF0000'},
									3: {targetAxisIndex: 0, color: '#004CFF'}
								},
								vAxes: {
									0: {
										title: 'Temperature (F)',
										format: 'decimal',
										textStyle: {color: '#004CFF'},
										titleTextStyle: {color: '#004CFF'},
										viewWindow: {min: 0}
									},
									1: {
										title: 'Fan Speed',
										format: 'decimal',
										textStyle: {color: '#FF0000'},
										titleTextStyle: {color: '#FF0000'},
										viewWindow: {min: 0}
									}
								},
								legend: {
									position: 'none'
								},
								chartArea: {
									width: '72%',
									height: '85%'
								}
							};
							var chart = new google.visualization.AreaChart(document.getElementById('chart_div'));
							chart.draw(data, options);
						}
					</script>
				</head>
				<body>
					<div id="chart_div"></div>
				</body>
			</html>
		"""
	render contentType: "text/html", data: html, status: 200
}