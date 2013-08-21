# Private EC2 Cloud Driver

> This cloud driver has been tested with Cloudify 2.6.1-b5199-139 available for download [here](https://s3-eu-west-1.amazonaws.com/cloudify-eu/gigaspaces-cloudify-2.6.1-ga-b5199-139.zip)

# Installation

Retrieve the privateEc2 project and perform the following maven command:
<pre><code>
mvn package -P cloudify -Dcloudify.home=$JSHOMEDIR
</code></pre>

This will package the project and copy all required files into Cloudify home directory defined by $JSHOMEDIR.
You will find the following files and directories :
<pre><code>
$JSHOMEDIR/cfn-templates/sampleApplication/*
$JSHOMEDIR/clouds/privateEc2/*
$JSHOMEDIR/lib/platform/esm/aws-java-sdk-1.4.7.jar
$JSHOMEDIR/lib/platform/esm/privateEc2-1.0-SNAPSHOT.jar
$JSHOMEDIR/recipes/apps/sampleApplication/*
$JSHOMEDIR/recipes/services/someService/*
</code>
</pre>

# Testing 

The following steps will guide you to configure and test the driver by deploying it in Amazon EC2. 

1. Initialize Amazon EC2:
	- Connect Amazon EC2 web interface : *https://console.aws.amazon.com/ec2/v2/home*. 
	- Ensure that you have a security groups named 'default' (*http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/using-network-security.html*) 
	- Configure the 'default' security groups to allow ports **8099** and **8100**.
2. Set properties: 
 	- Edit **$JSHOMEDIR/clouds/privateEc2/privateEc2-cloud.properties** and set Amazon credentials (accessKey, apiKey, keyFile and keyPair).
	- Edit **$JSHOMEDIR/cfn-templates/sampleApplication/someService-cfn.properties** (keyName).
3. Copy your Amazon keyFile into the folder **$JSHOMEDIR/clouds/privateEc2/upload**.
4. Bootstrap Cloudify :
	- run **$JSHOMEDIR/bin/cloudify.sh** 
	- type the command : <code>$bootstrap-cloud privateEc2</code>
5. Install the sample application : <code>$install-application -cloudConfiguration $path_to_cloudify/cfn-templates sampleApplication</code>
6. At the end of the deployment you should see '*sampleApplication*' deployed with a service '*someService*' and in the AWS management console you should see :
	- 1 instance of cloudify manager1
	- 1 instance of cloudify agent
	- 2 EBS attached to the agent ec2 instance.

By default, the deployment is done in **US-EAST-1**. If you want to deploy in another region edit the region and imageIds in the properties **privateEc2-cloud.properties** and **someService-cfn.properties**.<br />
It has been tested on Ubuntu VMs.

# Cloudformation templates format

The driver includes a custom simple parser of Cloudformation Templates.<br />
Fow now, it supports only 2 types of resources: AWS::EC2::Instance and AWS::EC2::Volume with a few functions (Base64, Join and Ref).