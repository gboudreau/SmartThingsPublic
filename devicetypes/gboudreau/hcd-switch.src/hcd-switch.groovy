/**
 *  HCD
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
metadata {
	definition (name: "HCD Switch", namespace: "gboudreau", author: "Guillaume Boudreau") {
    	capability "Switch"
        capability "Refresh"
	}

	simulator {
		// TODO: define status and reply messages here
	}

	tiles(scale: 1) {
        standardTile("switch", "device.switch", width: 3, height: 2, canChangeIcon: true) {
            state "off", label: 'OFF', action: "switch.on",
                  icon: "st.switches.light.off", backgroundColor: "#ffffff"
            state "on", label: 'ON', action: "switch.off",
                  icon: "st.switches.light.on", backgroundColor: "#79b821"
        }

        standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat", width: 1, height: 1) {
            state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
        }

        main("switch")
        details(["switch", "refresh"])
	}

    preferences {
    	input name: "hcd_api_endpoint_uri", type: "text", title: "HCD API Endpoint", description: "URI for the HCD SmartThings webhook endpoint.", required: true, displayDuringSetup: true
    	input name: "hcd_api_access_token", type: "text", title: "HCD API Access Token", description: "Bearer access token for the HCD API.", required: true, displayDuringSetup: true
	}

	command "on"
    command "off"
    command "refresh"
}

def on() {
	log.debug "HCD: Sending ON command for device ${device} (${device.deviceNetworkId})"
    api_call("on")
    api_call("get")
}

def off() {
	log.debug "HCD: Sending OFF command for device ${device} (${device.deviceNetworkId})"
    api_call("off")
    api_call("get")
}

def refresh() {
	api_call("get")
}

def api_call(action, value = null) {
	def json = [
    	action: action,
        device: device.deviceNetworkId
    ]
    if (value) {
    	json.value = value
    }
    def params = [
        uri: hcd_api_endpoint_uri,
        headers: ["HCD-Auth-ID": hcd_api_access_token]
    ]
    try {
        if (action == "get") {
        	params.query = json
	        httpGet(params) { resp ->
			    log.debug "HCD: Refreshed state ${resp.data}"
                sendEvent(name: "switch", value: resp.data.switch)
		    }
        } else {
        	params.body = json
	        httpPostJson(params)
        }
    } catch (groovyx.net.http.HttpResponseException ex) {
       	log.debug "HCD: Unexpected response from HCD API. Error: ${ex.statusCode}"
    } catch (all) {
       	log.debug "HCD: Exception: ${all}"
    }
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
}
