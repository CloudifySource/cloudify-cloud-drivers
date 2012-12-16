# IBM SmartCloud Cloud Driver ( for RedHat ) 

> This cloud driver has been tested with Cloudify 2.3.0 M1 (build 3481) available for download [here](http://repository.cloudifysource.org/org/cloudifysource/2.3.0-M1/gigaspaces-cloudify-2.3.0-m1-b3481.zip)
> Tested on image: 20070645 and location: 61

# Installation 

To install this driver following the following steps (make sure you have git client installed): 

* Clone the repo: 
<pre><code>
git clone git@github.com:CloudifySource/cloudify-cloud-drivers.git
</code></pre>

In the smartcloud root directory perform:
<pre><code>
mvn compile
mvn package
cp target/*.jar driver/upload/cloudify-overrides/lib/platform/esm
</code></pre>

* copy the cloud driver folder to the right location in the cloudify distro: 
<pre><code>
cp -r . &lt;cloudify root>/tools/cli/plugins/esc
</code></pre>

* copy the cloud driver jar file to lib/platform/esm: 
<pre><code>
cp target/*.jar &lt;cloudify root>/lib/platform/esm
</code></pre>


* Edit the file `smart-cloud.groovy` and add your cloud credentials instead of the place holders
<pre><code>
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
</code></pre>

* Place your key file (.pem file) in the upload directory of the cloud driver and update the relevant locations in the `smart-cloud.groovy` file with the key file's name

* Bootstrap the cloud: 
<pre><code>
cd <cloudify root>/bin
./cloudify.sh
</code></pre>

<pre><code>

  .oooooo.   oooo                              .o8   o8o   .o88o.             
 d8P'  `Y8b  `888                             "888   `"'   888 `"             
888           888   .ooooo.  oooo  oooo   .oooo888  oooo  o888oo  oooo    ooo 
888           888  d88' `88b `888  `888  d88' `888  `888   888     `88.  .8'  
888           888  888   888  888   888  888   888   888   888      `88..8'   
`88b    ooo   888  888   888  888   888  888   888   888   888       `888'    
 `Y8bood8P'  o888o `Y8bod8P'  `V88V"V8P' `Y8bod88P" o888o o888o       .8'     
                                                                  .o..P'      
                                                                  `Y8P'

  GigaSpaces Cloudify Shell.  

  Note for Windows Users:
   The Cloudify shell does not currently support the back-slash character ('\')
   as file separator. Instead, use the forward-slash character ('/') when
   specifying file paths.

Hit '<tab>' for a list of available commands.
Hit '[cmd] --help' for help on a specific command.
Hit '<ctrl-d>' or 'exit' to exit the console.

Cloudify version: 2.1.0


cloudify@default> bootstrap-cloud -timeout 30 --verbose smartcloud
</code></pre>

If you need more detailed please look at [OpenStack setup instructions](http://www.cloudifysource.org/guide/setup/configuring_openstack) in the cloudify documentation for agood reference example

