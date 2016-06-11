/**
 *  Generic Logger
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
    name: "Generic Logger",
    namespace: "gboudreau",
    author: "Guillaume Boudreau",
    description: "Send all events to a remote server, using JSON.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section ("Remote Server URL:") {
        input "remote_url", "text", title: "URL", required: true
    }
    section("Events to Log") {
        input "presences", "capability.presenceSensor", title: "Presence sensors:", multiple: true, required: false
        input "switches", "capability.switch", title: "Switches:", multiple: true, required: false
        input "levels", "capability.switchLevel", title: "Switch Levels:", multiple: true, required: false
        input "motions", "capability.motionSensor", title: "Motion sensors:", multiple: true, required: false
        input "temperatures", "capability.temperatureMeasurement", title: "Temperature sensors:", multiple: true, required: false
        input "humidities", "capability.relativeHumidityMeasurement", title: "Humidity sensors:", multiple: true, required: false
        input "contacts", "capability.contactSensor", title: "Contact sensors:", multiple: true, required: false
        input "alarms", "capability.alarm", title: "Alarms:", multiple: true, required: false
        input "indicators", "capability.indicator", title: "Indicators:", multiple: true, required: false
        input "codetectors", "capability.carbonMonoxideDetector", title: "CO detectors:", multiple: true, required: false
        input "smokedetectors", "capability.smokeDetector", title: "Smoke detectors:", multiple: true, required: false
        input "waterdetectors", "capability.waterSensor", title: "Water detectors:", multiple: true, required: false
        input "accelerations", "capability.accelerationSensor", title: "Acceleration sensors:", multiple: true, required: false
        input "energymeters", "capability.energyMeter", title: "Energy meters:", multiple: true, required: false
        input "musicplayer", "capability.musicPlayer", title: "Music players:", multiple: true, required: false
        input "powermeters", "capability.powerMeter", title: "Power meters:", multiple: true, required: false
        input "illuminances", "capability.illuminanceMeasurement", title: "Illuminance sensors:", multiple: true, required: false
        input "batteries", "capability.battery", title: "Batteries:", multiple: true, required: false
        input "button", "capability.button", title: "Buttons:", multiple: true, required: false
        input "voltage", "capability.voltageMeasurement", title: "Voltage sensors:", multiple: true, required: false
        input "lock", "capability.lock", title: "Locks:", multiple: true, required: false
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
    subscribe(alarms,             "alarm",                  genericHandler)
    subscribe(codetectors,        "carbonMonoxideDetector", genericHandler)
    subscribe(contacts,           "contact",                genericHandler)
    subscribe(indicators,         "indicator",              genericHandler)
    subscribe(modes,              "locationMode",           genericHandler)
    subscribe(motions,            "motion",                 genericHandler)
    subscribe(presences,          "presence",               genericHandler)
    subscribe(relays,             "relaySwitch",            genericHandler)
    subscribe(smokedetectors,     "smokeDetector",          genericHandler)
    subscribe(switches,           "switch",                 genericHandler)
    subscribe(levels,             "level",                  genericHandler)
    subscribe(temperatures,       "temperature",            genericHandler)
    subscribe(waterdetectors,     "water",                  genericHandler)
    subscribe(location,           "location",               genericHandler)
    subscribe(accelerations,      "acceleration",           genericHandler)
    subscribe(energymeters,       "energy",                 genericHandler)
    subscribe(musicplayers,       "music",                  genericHandler)
    subscribe(illuminaces,        "illuminance",            genericHandler)
    subscribe(powermeters,        "power",                  genericHandler)
    subscribe(batteries,          "battery",                genericHandler)
    subscribe(button,             "button",                 genericHandler)
    subscribe(voltageMeasurement, "voltage",                genericHandler)
    subscribe(lock,               "lock",                   genericHandler)
}

def genericHandler(evt) {
	def device_states = [:]
	def theAtts = evt.device.supportedAttributes
	theAtts.each {att ->
        def state = evt.device.currentState(att.name)
    	device_states[att.name] = state.value
	}

    def json = [
        date: evt.date,
        id: evt.id,
        data: evt.data,
        dateValue: evt.dateValue,
        description: evt.description,
        descriptionText: evt.descriptionText,
        device: [
        	id: evt.device.id,
        	displayName: evt.device.displayName,
            name: evt.device.name,
            states: device_states
        ],
        hubId: evt.hubId,
        installedSmartAppId: evt.installedSmartAppId,
        isDigital: evt.isDigital(),
        isoDate: evt.isoDate,
        isPhysical: evt.isPhysical(),
        isStateChange: evt.isStateChange(),
//        location: evt.location,
        locationId: evt.locationId,
        name: evt.name,
        source: evt.source,
        unit: evt.unit,
//        xyzValue: evt.xyzValue,
        value: evt.value
    ]

    def params = [
        uri: "${remote_url}",
        body: json
    ]
    try {
        httpPostJson(params)
    } catch (groovyx.net.http.HttpResponseException ex) {
       	log.debug "Unexpected response error: ${ex.statusCode}"
    } catch (all) {
       	log.debug "Exception: ${all}"
    }
}
