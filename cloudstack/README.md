 CloudStack Sudoer Cloud Driver ( for CentOs ) 

This cloud driver disables the requiretty flag in /etc/sudoers, so that Cloudify will be able to invoke remote ssh commands as a sudoer. This feature will be a part of Cloudify in the near future.

> This cloud driver has been tested with Cloudify 2.3.0 M1 available for download [here](http://repository.cloudifysource.org/org/cloudifysource/2.3.0-M1/gigaspaces-cloudify-2.3.0-m1-b3481.zip)

# Installation 

To install this driver following the following steps (make sure you have git client installed): 

* Clone the repo and copy the cloud driver folder to the right location in the cloudify distro: 
<pre><code>
git clone git@github.com:CloudifySource/cloudify-cloud-drivers.git
cp -r cloudstack/ <cloudify root>/tools/cli/plugins/esc
</code></pre>

* Edit the file `cloudstack-cloud.properties` and add your cloud credentials instead of the place holders

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

Cloudify version: 2.3.0


cloudify@default> bootstrap-cloud -timeout 20 cloudstack
</code></pre>

