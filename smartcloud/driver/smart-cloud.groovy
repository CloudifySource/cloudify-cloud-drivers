
cloud {
	// Mandatory. The name of the cloud, as it will appear in the Cloudify UI.
	name = "Smartcloud"
	configuration {
		// Mandatory - smartcloud Diablo cloud driver.
		className "org.cloudifysource.esc.driver.provisioning.smartcloud.SmartCloudDriver"
		// Optional. The template name for the management machines. Defaults to the first template in the templates section below.
		managementMachineTemplate "SMALL_LINUX"
		// Optional. Indicates whether internal cluster communications should use the machine private IP. Defaults to true.
		connectToPrivateIp false
	}

	provider {
		// optional 
		provider "smartcloud"
			
		// Optional. The HTTP/S URL where cloudify can be downloaded from by newly started machines. Defaults to downloading the
		// cloudify version matching that of the client from the cloudify CDN.
		// Change this if your compute nodes do not have access to an internet connection, or if you prefer to use a
		// different HTTP server instead.
		// 
		// cloudifyUrl "http://repository.cloudifysource.org/org/cloudifysource/2.3.0-M1/gigaspaces-cloudify-2.3.0-m1-b3481.zip"
		
		machineNamePrefix "cloudify_agent_"
		
		
		managementOnlyFiles ([])
		
		managementGroup "cloudify_manager"
		numberOfManagementMachines 1
		
		reservedMemoryCapacityPerMachineInMB 1024
		
		sshLoggingLevel "WARNING"
		
		
	}
	user {
		user "ENTER_USER"
		apiKey "ENTER_API_KEY"
		
	}
	templates ([
				SMALL_LINUX : template{
					username "idcuser"
					imageId "20070645"
					machineMemoryMB 1600
					hardwareId "BRZ64.2/4096/60*500*350"
					remoteDirectory "/home/idcuser/gs-files"
					localDirectory "upload"
					keyFile "ENTER_KEY_FILE"
					
					options ([
						"smartcloud.securityGroup" : "test",
						"smartcloud.keyPair" : "ENTER_KEY_PAIR_NAME"
					])
					
					// enable sudo.
					privileged true

					
				}
			])
			
	custom ([
		"smartcloud.endpoint" : "https://www-147.ibm.com/computecloud/enterprise/api/rest/20100331/",
		"smartcloud.location" : "61",
		"smartcloud.wireLog": "false"

	])
}

