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
	name = "rackspace"
	configuration {
		className "org.cloudifysource.esc.driver.provisioning.openstack.RSCloudDriver"
		managementMachineTemplate "SMALL_LINUX"
		connectToPrivateIp true
	}

	provider {
		provider "rackspace"
		localDirectory "tools/cli/plugins/esc/rsopenstack/upload"
		
		cloudifyUrl "http://repository.cloudifysource.org/org/cloudifysource/2.1.1/gigaspaces-cloudify-2.1.1-ga-b1396-361.zip" 
		//The machineNamePrefix property may not contain the char '_' in Rackspace
		machineNamePrefix "agent"
		
		dedicatedManagementMachines true
		managementOnlyFiles ([])
		
		//The managementGroup property may not contain the char '_' in Rackspace
		managementGroup "management"
		numberOfManagementMachines 1
		zones (["agent"])
		reservedMemoryCapacityPerMachineInMB 1024
		
		sshLoggingLevel "WARNING"
		
	}
	user {
		user "USER_NAME"
		apiKey "API_KEY"
	}
	templates ([
				SMALL_LINUX : template{
					imageId "118"
					machineMemoryMB 1600
					hardwareId "4"
					remoteDirectory "/root/gs-files"
					
				}
			])
			
	custom ([
		"openstack.endpoint" : "https://servers.api.rackspacecloud.com",
		"openstack.identity.endpoint": "https://auth.api.rackspacecloud.com/",
		"openstack.tenant" : "ENTER_TENANT",
		"openstack.wireLog": "false"

	])
}

