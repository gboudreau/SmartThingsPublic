/**
 *  Dryer Temperature Monitor
 *
 *  Copyright 2017 Guillaume Boudreau
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
    name: "Dryer Temperature Monitor",
    namespace: "gboudreau",
    author: "Guillaume Boudreau",
    description: "Send a notification when the dryer is done.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
    section ("Dryer Temperature Sensor") {
        input "meter", "capability.temperatureMeasurement", multiple: false, required: true
        input "cycle_start_temp_threshold", "number", title: "Cycle starts when temperature raises above (Celsius)", required: true, defaultValue: 30
        input "cycle_end_temp_threshold", "number", title: "Cycle is complete when temperature drops to (Celsius)", required: true, defaultValue: 30
    }

    section ("Send this message") {
        input "message", "text", title: "Notification message", required: true, capitalization: "sentences", defaultValue: "Dryer cycle completed"
    }

    section ("Notification method") {
        input "sendPushMessage", "bool", title: "Send a push notification?", defaultValue: true
    }

    section ("Additionally", hidden: hideOptionsSection(), hideable: true) {
        input "phone", "phone", title: "Send a text message to:", required: false
        input "switches", "capability.switch", title: "Turn on this switch", required:false, multiple:true
        input "hues", "capability.colorControl", title: "Turn these hue bulbs", required:false, multiple:true
        input "color", "enum", title: "This color", required: false, multiple:false, options: ["White", "Red","Green","Blue","Yellow","Orange","Purple","Pink"]
        input "lightLevel", "enum", title: "This light Level", required: false, options: [[10:"10%"],[20:"20%"],[30:"30%"],[40:"40%"],[50:"50%"],[60:"60%"],[70:"70%"],[80:"80%"],[90:"90%"],[100:"100%"]]
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
    subscribe(meter, "temperature", handler)
}

def handler(evt) {
    def latestTemperature = meter.currentValue("temperature")
    log.debug "Temperature: ${latestTemperature} Celsius"

    if (!state.cycleOn && latestTemperature > cycle_start_temp_threshold) {
        state.cycleOn = true
        state.cycleStart = now()
        log.debug "Cycle started."
    }
    else if (state.cycleOn && latestTemperature <= cycle_end_temp_threshold) {
        send(message)
        lightAlert(evt)
        state.cycleOn = false
        state.cycleEnd = now()
        def duration = (state.cycleEnd - state.cycleStart) / 60000
        log.debug "Cycle ended after ${duration} minutes."
  }
}

private lightAlert(evt) {
    def hueColor = 0
    def saturation = 100

    if (hues) {
        switch(color) {
            case "White":
            hueColor = 52
            saturation = 19
            break;
            case "Daylight":
            hueColor = 53
            saturation = 91
            break;
            case "Soft White":
            hueColor = 23
            saturation = 56
            break;
            case "Warm White":
            hueColor = 20
            saturation = 80 //83
            break;
            case "Blue":
            hueColor = 70
            break;
            case "Green":
            hueColor = 39
            break;
            case "Yellow":
            hueColor = 25
            break;
            case "Orange":
            hueColor = 10
            break;
            case "Purple":
            hueColor = 75
            break;
            case "Pink":
            hueColor = 83
            break;
            case "Red":
            hueColor = 100
            break;
        }

        state.previous = [:]

        hues.each {
            state.previous[it.id] = [
                "switch": it.currentValue("switch"),
                "level" : it.currentValue("level"),
                "hue": it.currentValue("hue"),
                "saturation": it.currentValue("saturation")
            ]
        }

        log.debug "current values = $state.previous"

        def newValue = [hue: hueColor, saturation: saturation, level: lightLevel as Integer ?: 100]
        log.debug "new value = $newValue"

        if (switches) {
            switches*.on()
        }
        hues*.setColor(newValue)
    }
}

private send(msg) {
    if (sendPushMessage) {
        sendPush(msg)
    }

    if (phone) {
        sendSms(phone, msg)
    }

    log.debug msg
}

private hideOptionsSection() {
    (phone || switches || hues || color || lightLevel) ? false : true
}
