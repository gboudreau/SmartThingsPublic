/**
 *  REST Access
 *
 *  Copyright 2016 Guillaume Boudreau
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
 */
definition(
    name: "REST Access",
    namespace: "gboudreau",
    author: "Guillaume Boudreau",
    description: "Allow me to control my things from PHP (using REST requests)",
    category: "SmartThings Labs",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: true)

preferences {
	section("Allow Endpoint to Control These Things...") {
        input "switches", "capability.switch", title: "Switches:", multiple: true, required: false
        input "levels", "capability.switchLevel", title: "Switch Levels:", multiple: true, required: false
	}
}

mappings {
	path("/switches") {
		action: [
			GET: "listSwitches"
		]
	}
	path("/switches/:id") {
		action: [
			GET: "showSwitch"
		]
	}
	path("/switches/:id/:command") {
		action: [
			GET: "updateSwitch"
		]
	}
	path("/levels") {
		action: [
			GET: "listLevels"
		]
	}
	path("/levels/:id") {
		action: [
			GET: "showLevel"
		]
	}
	path("/levels/:id/:command/:value") {
		action: [
			GET: "updateLevel"
		]
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	// TODO: subscribe to attributes, devices, locations, etc.
}

//switches
def listSwitches() {
	switches.collect{device(it,"switch")}
}

def showSwitch() {
	show(switches, "switch")
}
void updateSwitch() {
	update(switches)
}

//levels
def listLevels() {
	levels.collect{device(it, "level")}
}

def showLevel() {
	show(levels, "level")
}
void updateLevel() {
	update(levels)
}

def deviceHandler(evt) {}

private void update(devices) {
	log.debug "update, request: params: ${params}, devices: $devices.id"
    
    def command = params.command
    //let's create a toggle option here
	if (command) 
    {
		def device = devices.find { it.id == params.id }
		if (!device) {
			httpError(404, "Device not found")
		} else {
        	if(command == "toggle")
       		{
            	if(device.currentValue('switch') == "on")
                  device.off();
                else
                  device.on();
       		}
       		else
       		{
            	def value = params.value
                if (value) {
                	if (command == "level") {
                    	command = "setLevel"
                    }
                	if (command == "hue") {
                    	command = "setHue"
                    }
                	if (command == "saturation") {
                    	command = "setSaturation"
                    }
                	if (command == "colorTemperature") {
                    	command = "setColorTemperature"
                    }
                	if (command == "color") {
                    	command = "setColor"
                    }
					log.debug "calling device.$command($value)"
					device."$command"(value as int)
                } else {
					log.debug "calling device.$command()"
					device."$command"()
                }
            }
		}
	}
}

private show(devices, type) {
	def device = devices.find { it.id == params.id }
	if (!device) {
		httpError(404, "Device not found")
	} else {
		def attributeName = type == "motionSensor" ? "motion" : type
		def s = device.currentState(attributeName)
		[id: device.id, label: device.displayName, value: s?.value, unitTime: s?.date?.time, type: type]
	}
}

private device(it, type) {
	it ? [id: it.id, label: it.label, type: type] : null
}
