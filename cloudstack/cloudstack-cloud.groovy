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
	name = "cloudstack"
	configuration {
		className "org.cloudifysource.esc.driver.provisioning.jclouds.DefaultProvisioningDriver"
		managementMachineTemplate "SMALL_LINUX"
		connectToPrivateIp false
		bootstrapManagementOnPublicIp true
	}

	provider {
		provider "cloudstack"
//		cloudifyUrl "http://repository.cloudifysource.org/org/cloudifysource/2.3.0-M1/gigaspaces-cloudify-2.3.0-m1-b3481" 
		machineNamePrefix INSTANCE_NAME_PREFIX		
		dedicatedManagementMachines true
		managementOnlyFiles ([])		

		sshLoggingLevel "WARNING"
		managementGroup MANAGEMENT_NAME_PREFIX
		numberOfManagementMachines 1
		reservedMemoryCapacityPerMachineInMB 1024
		
	}
	
	user {
		user CLOUDSTACK_API_KEY
		apiKey CLOUDSTACK_SECRET_KEY
	}
	
	templates ([
				SMALL_LINUX : template{            
					imageId SMALL_LINUX_IMAGE_ID
					hardwareId SMALL_LINUX_HARDWARE_ID
					machineMemoryMB 1600
					locationId SMALL_LINUX_LOCATION_ID
					remoteDirectory "/root/gs-files"
					localDirectory "upload"
					username SMALL_LINUX_SSH_USER_NAME

					options   ([ 
						"networkId" : SMALL_LINUX_NETWORK_ID ,
						"setupStaticNat" : false
					])

					overrides (["jclouds.endpoint" : CLOUDSTACK_ENDPOINT])
					privileged true
				}				
	])
}


