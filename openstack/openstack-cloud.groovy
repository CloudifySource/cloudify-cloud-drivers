/*******************************************************************************
* Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*******************************************************************************/
cloud {
	// Mandatory. The name of the cloud, as it will appear in the Cloudify UI.
	name = "Openstack"
	configuration {
		// Mandatory - openstack Diablo cloud driver.
		className "org.cloudifysource.esc.driver.provisioning.openstack.OpenstackCloudDriver"
		// Optional. The template name for the management machines. Defaults to the first template in the templates section below.
		managementMachineTemplate "SMALL_LINUX"
		// Optional. Indicates whether internal cluster communications should use the machine private IP. Defaults to true.
		connectToPrivateIp true
	}

	provider {
		// optional 
		provider "openstack"
		localDirectory "tools/cli/plugins/esc/openstack/upload"
		
		cloudifyUrl "http://repository.cloudifysource.org/org/cloudifysource/2.1.1/gigaspaces-cloudify-2.1.1-ga-b1396-361.zip" 
		machineNamePrefix "cloudify_agent_"
		
		dedicatedManagementMachines true
		managementOnlyFiles ([])
		
		managementGroup "cloudify_manager"
		numberOfManagementMachines 1
		zones (["agent"])
		reservedMemoryCapacityPerMachineInMB 1024
		
		sshLoggingLevel "WARNING"
		
		
	}
	user {
		user "ENTER_USER"
		apiKey "ENTER_API_KEY"
		keyFile "ENTER_KEY_FILE"
	}
	templates ([
				SMALL_LINUX : template{
                    imageId "1234"
					machineMemoryMB 3200
					hardwareId "103"
					remoteDirectory "/root/gs-files"					
					options ([
						"openstack.securityGroup" : "default",
						"openstack.keyPair" : "ENTER_KEY_PAIR_NAME"
					])
					
				}
			])
			
	custom ([
		"openstack.endpoint" : "https://az-1.region-a.geo-1.compute.hpcloudsvc.com/",
		"openstack.identity.endpoint": "https://region-a.geo-1.identity.hpcloudsvc.com:35357/",
        "openstack.tenant" : "ENTER_TENANT",
		"openstack.wireLog": "false"

	])
}

