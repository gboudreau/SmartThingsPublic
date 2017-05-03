/**
 *  Hunter Douglas PowerView
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
metadata {
	definition (name: "Hunter Douglas PowerView", namespace: "gboudreau", author: "Guillaume Boudreau") {
		capability "Window Shade"
    	capability "Switch"
	}

	simulator {
		// TODO: define status and reply messages here
	}

	tiles {
        valueTile("Status", "device.status", width: 3, height: 1) {
            state "status", label: '${currentValue}'
        }
		standardTile("openSwitch", "device.switch", width: 1, height: 1, canChangeIcon: true) {
			state "nc", label: 'Open', action: "open",
            icon: "st.doors.garage.garage-open"
		}
		standardTile("closeSwitch", "device.switch", width: 1, height: 1, canChangeIcon: true) {
			state "nc", label: 'Close', action: "close",
            icon: "st.doors.garage.garage-closed"
		}
		standardTile("presetSwitch", "device.switch", width: 1, height: 1, canChangeIcon: true) {
			state "nc", label: 'Preset', action: "presetPosition",
            icon: "st.doors.garage.garage-opening"
		}
        standardTile("switch", "device.switch", width: 3, height: 1, canChangeIcon: true) {
            state "off", label: 'OFF', action: "switch.on",
                  icon: "st.switches.light.off", backgroundColor: "#ffffff"
            state "on", label: 'ON', action: "switch.off",
                  icon: "st.switches.light.on", backgroundColor: "#79b821"
        }
		details(["Status","openSwitch","closeSwitch","presetSwitch","switch"])
        main ("switch")
	}

    preferences {
        input name: "hub_ip_address", type: "text", title: "IP Address", description: "IP address of your HD PowerView Hub", 
        	displayDuringSetup: true, required: true
        input name: "hub_tcp_port", type: "number", title: "Port", description: "TCP port number of your HD PowerView Hub", 
        	displayDuringSetup: true, required: true
        input name: "access_token", type: "text", title: "Authorization Token", description: "Token sent as a Bearer token (using HTTP 'Authorization' header)", 
        	displayDuringSetup: true, required: true
        input name: "scene_open", type: "text", title: "Open Scene", description: "Name of the scene to open the shades", 
        	displayDuringSetup: true, required: true
        input name: "scene_close", type: "text", title: "Close Scene", description: "Name of the scene to close the shades", 
        	displayDuringSetup: true, required: true
        input name: "scene_preset", type: "text", title: "Preset (custom) Scene", description: "Name of the scene to set the shades to a custom (preset) position", 
        	displayDuringSetup: true, required: false
    }
	command "open"
    command "close"
    command "preset"
	command "on"
    command "off"
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
}

// handle commands
def open() {
	log.debug "Executing 'open'"
    api_call(scene_open, "Open")
}
def off() {
    sendEvent(name: "status", value: "off")
	open()
}

def close() {
	log.debug "Executing 'close'"
    api_call(scene_close, "Close")
}
def on() {
    sendEvent(name: "status", value: "on")
	close()
}

def presetPosition() {
	log.debug "Executing 'presetPosition'"
    api_call(scene_preset, "Preset")
}

def api_call(scene, new_status) {
    def params = [
        uri: "http://${hub_ip_address}:${hub_tcp_port}/api/scenes/"
    ]
    if (access_token && access_token != "") {
        params.headers = ["Authorization": "Bearer ${access_token}"]
    }
    log.debug "HDPV: Using URL '${params.uri}'"
    if (!state.known_scenes) {
        state.known_scenes = [a: 0]
    }
    if (state.known_scenes[scene]) {
	    change_scene(params, state.known_scenes[scene], new_status)
        return
    }
    try {
        httpGet(params) { resp ->
            log.debug "HDPV: Get Scenes response: ${resp.data}"
            for (sceneData in resp.data.sceneData) {
            	def base64Name = sceneData.name
                byte[] decoded = base64Name.decodeBase64()
                def sceneName = new String(decoded)
	            log.debug "HDPV: Scene '${sceneName}' = ${scene} ?"
                if (sceneName == scene) {
                    state.known_scenes[scene] = sceneData.id
	                change_scene(params, sceneData.id, new_status)
                    break
                }
            }
        }
    } catch (groovyx.net.http.HttpResponseException ex) {
       	log.debug "HDPV: Unexpected response from HD Hub. Error: ${ex.statusCode}"
    } catch (all) {
       	log.debug "HDPV: Exception: ${all}"
    }
}

def change_scene(params, scene_id, new_status) {
    params.query = [
        sceneid: scene_id
    ]
    httpGet(params) { resp2 ->
        log.debug "HDPV: Change scene response: ${resp2.data}"
        sendEvent(name: "status", value: new_status)
    }
}