# Cloudify Ibm SmartClouad driver.

This directory contains the laitest development of the source and configuration needed for the Ibm SmartClouad POC.
On the src directory there are the driver Java source files on the following path:
src\main\java\org\cloudifysource\esc\driver\provisioning\smartcloud

On the driver directory there are the configuration files & jars that must be located on the cloudify build in the following location:
\<CLOUDIFY_HOME\>\tools\cli\plugins\esc

The upload directory contains SmartCloud.jar file which is curently uptodate with the existed java source files.
The SmartCloud.jar also needs to be copied to:
\<CLOUDIFY_HOME\>\lib\platform\esm

* Note: For any change in the driver source code a new SmartCloud.jar must be created and must overwrite the older version in the upload directory.
