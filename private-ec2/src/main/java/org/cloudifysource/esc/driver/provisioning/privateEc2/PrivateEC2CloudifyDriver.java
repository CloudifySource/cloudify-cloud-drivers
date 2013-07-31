/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 ******************************************************************************/
package org.cloudifysource.esc.driver.provisioning.privateEc2;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.io.FileUtils;
import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.dsl.cloud.CloudUser;
import org.cloudifysource.dsl.cloud.ScriptLanguages;
import org.cloudifysource.dsl.cloud.compute.ComputeTemplate;
import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;
import org.cloudifysource.esc.driver.provisioning.MachineDetails;
import org.cloudifysource.esc.driver.provisioning.ProvisioningContext;
import org.cloudifysource.esc.driver.provisioning.ProvisioningContextAccess;
import org.cloudifysource.esc.driver.provisioning.jclouds.DefaultProvisioningDriver;
import org.cloudifysource.esc.driver.provisioning.privateEc2.parser.ParserUtils;
import org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans.AWSEC2Instance;
import org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans.AWSEC2Volume;
import org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans.InstanceProperties;
import org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans.PrivateEc2Template;
import org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans.VolumeMapping;
import org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans.VolumeProperties;
import org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans.types.ValueType;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.BlockDeviceMapping;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeTagsRequest;
import com.amazonaws.services.ec2.model.DescribeTagsResult;
import com.amazonaws.services.ec2.model.DescribeVolumesRequest;
import com.amazonaws.services.ec2.model.DescribeVolumesResult;
import com.amazonaws.services.ec2.model.EbsBlockDevice;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Placement;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.TagDescription;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesResult;
import com.amazonaws.services.ec2.model.Volume;

/**
 * A custom Cloud Driver to provision Amazon EC2 machines using cloud formation templates.<br />
 * This driver can still start machines the usual way using cloudify groovy templates.
 * 
 * @author victor
 * 
 */
public class PrivateEC2CloudifyDriver extends DefaultProvisioningDriver {

	private static final long WAIT_STATUS_SLEEP_TIME = 2000L;
	private static final String PATTERN_PROPS_JSON = "\\s*\\\"[\\w-]*\\\"\\s*:\\s*([^{(\\[\"][\\w-]+)\\s*,?";
	private static final String VOLUME_PREFIX = "cloudify-storage-";

	/** Key name for amazon tag resource's name. */
	private static final String TK_NAME = "Name";

	/**
	 * Enumeration for supported 'resource-type' value used in com.amazonaws.services.ec2.model.Filter parameter.
	 */
	private static enum TagResourceType {
		INSTANCE, VOLUME;
		public String getValue() {
			return name().toLowerCase();
		}
	}

	/** Counter for storage instances. */
	protected static AtomicInteger volumeCounter = new AtomicInteger(0);

	/** Map which contains all parsed CFN template. */
	private final Map<String, PrivateEc2Template> cfnTemplatePerService = new HashMap<String, PrivateEc2Template>();

	private AmazonEC2 ec2;

	/** short name of the service (i.e without applicationName). */
	private String serviceName;

	/**
	 * *****************************************************************************************************************
	 * * ***************************************************************************************************************
	 * ***
	 */

	/**
	 * Sets the custom data file for the cloud driver instance of a specific service.<br />
	 * <p>
	 * customDataFile is a simple file or a folder like the following:
	 * 
	 * <pre>
	 *   -- applicationNameFolder --- serviceName1-cfn.template
	 *                             |- serviceName2-cfn.template
	 *                             |- serviceName3-cfn.template
	 * </pre>
	 * 
	 * </p>
	 * 
	 * @param customDataFile
	 *            files or directory containing the amazon cloudformation template of a specific service
	 * 
	 */
	@Override
	public void setCustomDataFile(final File customDataFile) {
		super.setCustomDataFile(customDataFile);

		final Map<String, PrivateEc2Template> map = new HashMap<String, PrivateEc2Template>();
		PrivateEc2Template mapJson = null;

		try {
			if (customDataFile.isFile()) {
				String templateName = this.getTemplatName(customDataFile);
				logger.fine("Parsing CFN Template for service=" + templateName);
				mapJson = ParserUtils.mapJson(PrivateEc2Template.class, customDataFile);
				map.put(templateName, mapJson);
			} else {
				File[] listFiles = customDataFile.listFiles();
				if (listFiles != null) {
					for (File file : listFiles) {
						if (this.isTemplateFile(file)) {
							String templateName = this.getTemplatName(file);
							logger.fine("Parsing CFN Template for service=" + templateName);

							File pFile = this.getPropertiesFileIfExists(templateName, customDataFile.listFiles());
							if (pFile != null) {
								// Replace properties variable with values if the properties file exists
								String templateString = this.replaceProperties(file, pFile);
								mapJson = ParserUtils.mapJson(PrivateEc2Template.class, templateString);
								map.put(templateName, mapJson);

							} else {
								mapJson = ParserUtils.mapJson(PrivateEc2Template.class, file);
								map.put(templateName, mapJson);
							}
						}
					}
				}
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Couldn't parse the template file: " + customDataFile.getPath());
			throw new IllegalStateException(e);
		}
		this.cfnTemplatePerService.putAll(map);
	}

	private String replaceProperties(final File file, final File propertiesFile) throws IOException {
		logger.fine("Properties file=" + propertiesFile.getName());
		Properties props = new Properties();
		props.load(new FileInputStream(propertiesFile));

		String templateString = FileUtils.readFileToString(file);

		Pattern p = Pattern.compile(PATTERN_PROPS_JSON);
		Matcher m = p.matcher(templateString);
		while (m.find()) {
			String group = m.group();
			String group1 = m.group(1);
			if (props.containsKey(group1)) {
				String value = props.getProperty(group1);
				if (logger.isLoggable(Level.FINEST)) {
					logger.finest("Replacing property " + group + " by " + value);
				}
				templateString = m.replaceFirst(group.replace(group1, value));
				m = p.matcher(templateString);
			} else {
				throw new IllegalStateException("Couldn't find property: " + group1);
			}
		}
		return templateString;
	}

	private File getPropertiesFileIfExists(final String templateName, final File[] listFiles) {
		String filename = templateName + "-cfn.properties";
		for (File file : listFiles) {
			if (filename.equals(file.getName())) {
				return file;
			}
		}
		return null;
	}

	private String getTemplatName(final File file) {
		String name = file.getName();
		name = name.replace("-cfn.template", "");
		return name;
	}

	private boolean isTemplateFile(final File file) {
		String name = file.getName();
		return name.endsWith("-cfn.template");
	}

	/** Testing purpose. */
	PrivateEc2Template getCFNTemplatePerService(final String serviceName) {
		return cfnTemplatePerService.get(serviceName);
	}

	public Cloud getCloud() {
		return this.cloud;
	}

	/**
	 * *****************************************************************************************************************
	 */

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.cloudifysource.esc.driver.provisioning.BaseProvisioningDriver#setConfig(org.cloudifysource.dsl.cloud.Cloud,
	 * java.lang.String, boolean, java.lang.String)
	 */
	@Override
	public void setConfig(final Cloud cloud, final String cloudTemplateName, final boolean management,
			final String fullServiceName) throws CloudProvisioningException {
		this.serviceName = this.getSimpleServiceName(fullServiceName);
		super.setConfig(cloud, cloudTemplateName, management, fullServiceName);

		if (logger.isLoggable(Level.FINER)) {
			logger.finer("Service name : " + this.serviceName + "(" + fullServiceName + ")");
		}

		// Initialize the ec2 client if the service use the CFN template
		if (this.useCloudFormationTemplate()) {
			logger.info("Use custom EC2 driver");
			this.ec2 = this.createAmazonEC2();
		}
	}

	/**
	 * Checks if the service uses a CFN template for provisioning.<br />
	 * It supposes that the field serciceName has been initialized.
	 */
	private boolean useCloudFormationTemplate() {
		return this.cfnTemplatePerService.containsKey(this.serviceName);
	}

	/**
	 * Remove application name from the string.<br />
	 * i.e. if fullServiceName = sampleApplication.someService, it will return someService.
	 * 
	 * @param fullServiceName
	 *            A service name.
	 * @return The service name shortened by the application name.
	 */
	private String getSimpleServiceName(final String fullServiceName) {
		if (fullServiceName != null && fullServiceName.contains(".")) {
			return fullServiceName.substring(fullServiceName.lastIndexOf(".") + 1, fullServiceName.length());
		}
		return fullServiceName;
	}

	@Override
	protected void initDeployer(final Cloud cloud) {
		if (!this.useCloudFormationTemplate()) {
			super.initDeployer(cloud);
		}
	}

	private AmazonEC2 createAmazonEC2() throws CloudProvisioningException {
		CloudUser user = cloud.getUser();
		AWSCredentials credentials = new BasicAWSCredentials(user.getUser(), user.getApiKey());

		Region region = this.getRegion();

		AmazonEC2 ec2 = new AmazonEC2Client(credentials);

		// If region is not set, take the manager one
		if (region != null) {
			logger.info("Amazon ec2 use region: " + region);
			ec2.setRegion(region);
		} else {
			ComputeTemplate computeTemplate =
					cloud.getCloudCompute().getTemplates().get(cloud.getConfiguration().getManagementMachineTemplate());
			Region endpointRegion = RegionUtils.convertLocationId2Region(computeTemplate.getLocationId());
			logger.info("Amazon ec2 use cloudify manager region:" + region);
			ec2.setRegion(endpointRegion);
		}
		return ec2;
	}

	private Region getRegion() {
		PrivateEc2Template cfnTemplate = this.cfnTemplatePerService.get(this.serviceName);
		AWSEC2Instance instance = (AWSEC2Instance) cfnTemplate.getEC2Instance();
		ValueType availabilityZoneObj = instance.getProperties().getAvailabilityZone();

		Region region = null;
		if (availabilityZoneObj != null) {
			region = RegionUtils.convertAvailabilityZone2Region(availabilityZoneObj.getValue());
		}

		return region;
	}

	/**
	 * *****************************************************************************************************************
	 */

	/**
	 * Start machines using CFN template if provides, use JClouds otherwise.
	 * 
	 * @param locationId
	 *            the location to allocate the machine to.
	 * @param duration
	 *            Time duration to wait for the instance.
	 * @param unit
	 *            Time unit to wait for the instance.
	 * 
	 * @return The details of the started instance.
	 * 
	 * @throws TimeoutException
	 *             In case the instance was not started in the allotted time.
	 * @throws CloudProvisioningException
	 *             If a problem was encountered while starting the machine.
	 */
	@Override
	public MachineDetails startMachine(final String locationId, final long duration, final TimeUnit unit)
			throws TimeoutException, CloudProvisioningException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("Stating new machine with the following thread: threadId=" + Thread.currentThread().getId()
					+ " serviceName=" + this.serviceName);
		}

		MachineDetails md = null;
		if (this.useCloudFormationTemplate()) {
			md = this.startMachineWithAmazonSDK(duration, unit);
		} else {
			md = super.startMachine(locationId, duration, unit);
		}
		return md;
	}

	@Override
	public boolean stopMachine(final String serverIp, final long duration, final TimeUnit unit)
			throws CloudProvisioningException,
			TimeoutException, InterruptedException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("Stopping new machine with the following thread: threadId=" + Thread.currentThread().getId()
					+ " serviceName=" + this.serviceName
					+ " serverIp=" + serverIp);
		}

		boolean stopped = false;
		if (this.useCloudFormationTemplate()) {
			stopped = this.stopMachineWithAmazonSDK(serverIp, duration, unit);
		} else {
			stopped = super.stopMachine(serverIp, duration, unit);
		}
		return stopped;
	}

	boolean stopMachineWithAmazonSDK(final String serverIp, final long duration, final TimeUnit unit)
			throws CloudProvisioningException,
			TimeoutException {
		logger.info("Stopping instance server ip = " + serverIp + "...");
		DescribeInstancesRequest describeInstance = new DescribeInstancesRequest();
		describeInstance.withFilters(new Filter("private-ip-address", Arrays.asList(serverIp)));
		DescribeInstancesResult describeInstances = ec2.describeInstances(describeInstance);

		Reservation reservation = describeInstances.getReservations().get(0);
		if (reservation != null && reservation.getInstances().get(0) != null) {
			TerminateInstancesRequest tir = new TerminateInstancesRequest();
			tir.withInstanceIds(reservation.getInstances().get(0).getInstanceId());
			TerminateInstancesResult terminateInstances = ec2.terminateInstances(tir);

			String instanceId = terminateInstances.getTerminatingInstances().get(0).getInstanceId();

			try {
				this.waitStopInstanceStatus(instanceId, duration, unit);
			} finally {
				// FIXME By default, cloudify doesn't delete tags. So we should keep it that way.
				// Remove instance Tags
				// if (!terminateInstances.getTerminatingInstances().isEmpty()) {
				// logger.fine("Deleting tags for instance id=" + instanceId);
				// DeleteTagsRequest deleteTagsRequest = new DeleteTagsRequest();
				// deleteTagsRequest.setResources(Arrays.asList(instanceId));
				// ec2.deleteTags(deleteTagsRequest);
				// }
			}

		} else {
			logger.warning("No instance to stop: " + reservation);
		}
		return true;
	}

	private void waitStopInstanceStatus(final String instanceId, final long duration, final TimeUnit unit)
			throws CloudProvisioningException, TimeoutException {
		long endTime = System.currentTimeMillis() + unit.toMillis(duration);
		while (System.currentTimeMillis() < endTime) {

			DescribeInstancesRequest describeRequest = new DescribeInstancesRequest();
			describeRequest.withInstanceIds(instanceId);
			DescribeInstancesResult describeInstances = ec2.describeInstances(describeRequest);

			for (Reservation resa : describeInstances.getReservations()) {
				for (Instance instance : resa.getInstances()) {
					InstanceStateType state = InstanceStateType.valueOf(instance.getState().getCode());
					if (logger.isLoggable(Level.FINEST)) {
						logger.finest("instance= " + instance.getInstanceId() + " state=" + state);
					}
					switch (state) {
					case PENDING:
					case RUNNING:
					case STOPPING:
					case SHUTTING_DOWN:
						try {

							if (logger.isLoggable(Level.FINEST)) {
								logger.finest("sleeping...");
							}
							Thread.sleep(WAIT_STATUS_SLEEP_TIME);
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
						}
						break;
					case STOPPED:
					case TERMINATED:
						if (logger.isLoggable(Level.FINEST)) {
							logger.finest("instance (id=" + instanceId + ") was shutdown");
						}
						return;
					default:
						throw new CloudProvisioningException("Failed to stop server - Cloud reported node in "
								+ state.getName() + " state.");

					}

				}
			}
		}

		throw new TimeoutException("Stopping instace timed out (id=" + instanceId + ")");
	}

	/**
	 * Start machine using CFN template.
	 * 
	 * @param duration
	 *            Time duration to wait for the instance.
	 * @param unit
	 *            Time unit to wait for the instance.
	 * 
	 * @return The details of the started instance.
	 * 
	 * @throws TimeoutException
	 *             In case the instance was not started in the allotted time.
	 * @throws CloudProvisioningException
	 *             If a problem was encountered while starting the machine.
	 */
	public MachineDetails startMachineWithAmazonSDK(final long duration, final TimeUnit unit)
			throws CloudProvisioningException, TimeoutException {

		PrivateEc2Template cfnTemplate = cfnTemplatePerService.get(this.serviceName);

		Instance ec2Instance = this.createEC2Instance(cfnTemplate);
		ec2Instance = this.waitRunningInstance(ec2Instance, duration, unit);

		this.tagEC2Instance(ec2Instance, cfnTemplate.getEC2Instance());
		this.tagEC2Volumes(ec2Instance.getInstanceId(), cfnTemplate);

		MachineDetails md = new MachineDetails();
		md.setMachineId(ec2Instance.getInstanceId());
		md.setPrivateAddress(ec2Instance.getPrivateIpAddress());
		md.setPublicAddress(ec2Instance.getPublicIpAddress());
		md.setAgentRunning(true);
		md.setCloudifyInstalled(true);

		logger.fine("[" + md.getMachineId() + "] Cloud Server is allocated.");

		return md;
	}

	private void tagEC2Instance(final Instance ec2Instance, final AWSEC2Instance templateInstance)
			throws CloudProvisioningException {
		String ec2InstanceName =
				this.createNewName(TagResourceType.INSTANCE, cloud.getProvider().getMachineNamePrefix());
		List<Tag> additionalTags = Arrays.asList(new Tag(TK_NAME, ec2InstanceName));
		this.createEC2Tags(ec2Instance.getInstanceId(), templateInstance.getProperties().getTags(), additionalTags);
	}

	private void tagEC2Volumes(final String instanceId, final PrivateEc2Template cfnTemplate)
			throws CloudProvisioningException {

		List<VolumeMapping> volumeMappings = cfnTemplate.getEC2Instance().getProperties().getVolumes();
		if (volumeMappings != null) {
			DescribeVolumesRequest request = new DescribeVolumesRequest();
			request.withFilters(new Filter("attachment.instance-id", Arrays.asList(instanceId)));
			DescribeVolumesResult describeVolumes = ec2.describeVolumes(request);

			for (Volume volume : describeVolumes.getVolumes()) {
				String volumeRef = null;
				for (VolumeMapping vMap : volumeMappings) {
					String device = volume.getAttachments().get(0).getDevice();
					if (device.equals(vMap.getDevice().getValue())) {
						volumeRef = vMap.getVolumeId().getValue();
						break;
					}
				}
				if (volumeRef != null) {
					AWSEC2Volume ec2Volume = cfnTemplate.getEC2Volume(volumeRef);
					List<org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans.Tag> templateTags =
							ec2Volume == null ? null : ec2Volume
									.getProperties().getTags();
					List<Tag> additionalTags =
							Arrays.asList(new Tag(TK_NAME, this.createNewName(TagResourceType.VOLUME, VOLUME_PREFIX)));
					this.createEC2Tags(volume.getVolumeId(), templateTags, additionalTags);
				}
			}
		}
	}

	private String createNewName(final TagResourceType resourceType, final String prefix)
			throws CloudProvisioningException {
		String newName = null;
		int attempts = 0;
		boolean foundFreeName = false;

		while (attempts < MAX_SERVERS_LIMIT) {
			// counter = (counter + 1) % MAX_SERVERS_LIMIT;
			++attempts;

			switch (resourceType) {
			case INSTANCE:
				newName = prefix + counter.incrementAndGet();
				break;
			case VOLUME:
				newName = prefix + volumeCounter.incrementAndGet();
				break;
			default:
				// not possible
				throw new CloudProvisioningException("ResourceType not supported");
			}

			// verifying this server name is not already used
			DescribeTagsRequest tagRequest = new DescribeTagsRequest();
			tagRequest.withFilters(new Filter("resource-type", Arrays.asList(resourceType.getValue())));
			tagRequest.withFilters(new Filter("value", Arrays.asList(newName)));
			DescribeTagsResult describeTags = ec2.describeTags(tagRequest);
			List<TagDescription> tags = describeTags.getTags();
			if (tags == null || tags.isEmpty()) {
				foundFreeName = true;
				break;
			}
		}

		if (!foundFreeName) {
			throw new CloudProvisioningException("Number of servers has exceeded allowed server limit ("
					+ MAX_SERVERS_LIMIT + ")");
		}
		return newName;
	}

	private void createEC2Tags(final String resourceId,
			final List<org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans.Tag> templateTags,
			final List<Tag> additionalTags) {
		List<Tag> tags = new ArrayList<Tag>();

		if (templateTags != null) {
			for (org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans.Tag tag : templateTags) {
				tags.add(tag.convertToEC2Model());
			}
		}

		if (additionalTags != null) {
			tags.addAll(additionalTags);
		}

		if (!tags.isEmpty()) {
			logger.fine("Tag resourceId=" + resourceId + " tags=" + tags);
			CreateTagsRequest ctr = new CreateTagsRequest();
			ctr.setTags(tags);
			ctr.withResources(resourceId);
			this.ec2.createTags(ctr);
		}
	}

	private Instance waitRunningInstance(final Instance ec2instance, final long duration, final TimeUnit unit)
			throws CloudProvisioningException,
			TimeoutException {

		long endTime = System.currentTimeMillis() + unit.toMillis(duration);

		while (System.currentTimeMillis() < endTime) {
			DescribeInstancesRequest describeRequest = new DescribeInstancesRequest();
			describeRequest.setInstanceIds(Arrays.asList(ec2instance.getInstanceId()));
			DescribeInstancesResult describeInstances = this.ec2.describeInstances(describeRequest);

			for (Reservation resa : describeInstances.getReservations()) {
				for (Instance instance : resa.getInstances()) {
					InstanceStateType state = InstanceStateType.valueOf(instance.getState().getCode());
					if (logger.isLoggable(Level.FINER)) {
						logger.finer("instance= " + instance.getInstanceId() + " state=" + state);
					}
					switch (state) {
					case PENDING:
						try {
							if (logger.isLoggable(Level.FINEST)) {
								logger.finest("sleeping...");
							}
							Thread.sleep(WAIT_STATUS_SLEEP_TIME);
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
						}
						break;
					case RUNNING:
						logger.fine("running okay...");
						return instance;
					case STOPPING:
					case SHUTTING_DOWN:
					case TERMINATED:
					case STOPPED:
					default:
						throw new CloudProvisioningException("Failed to allocate server - Cloud reported node in "
								+ state.getName() + " state. Node details: "
								+ ec2instance);

					}

				}
			}
		}

		throw new TimeoutException("Node failed to reach RUNNING mode in time");
	}

	private MachineDetails[] getManagementServersMachineDetails() throws CloudProvisioningException {

		DescribeInstancesResult describeInstances = this.requestEC2InstancesManager();
		List<MachineDetails> mds = new ArrayList<MachineDetails>();
		for (Reservation resa : describeInstances.getReservations()) {
			for (Instance instance : resa.getInstances()) {
				MachineDetails md = this.createMachineDetailsFromInstance(instance);
				mds.add(md);
			}
		}

		return mds.toArray(new MachineDetails[mds.size()]);
	}

	private DescribeInstancesResult requestEC2InstancesManager() {
		DescribeTagsRequest tagRequest = new DescribeTagsRequest();
		tagRequest.withFilters(new Filter("resource-type", Arrays.asList("instance")));
		tagRequest.withFilters(new Filter("value", Arrays.asList(cloud.getProvider().getManagementGroup() + "*")));
		DescribeTagsResult describeTags = ec2.describeTags(tagRequest);
		List<TagDescription> tags = describeTags.getTags();

		List<String> managerIds = new ArrayList<String>(tags.size());
		for (TagDescription tag : tags) {
			managerIds.add(tag.getResourceId());
		}

		DescribeInstancesRequest request = new DescribeInstancesRequest();
		request.withInstanceIds(managerIds);
		DescribeInstancesResult describeInstances = ec2.describeInstances(request);
		return describeInstances;
	}

	private MachineDetails createMachineDetailsFromInstance(final Instance instance) throws CloudProvisioningException {
		final ComputeTemplate template = this.cloud.getCloudCompute().getTemplates().get(
				this.cloudTemplateName);

		if (template == null) {
			throw new CloudProvisioningException("Could not find template " + this.cloudTemplateName);
		}

		final MachineDetails md = super.createMachineDetailsForTemplate(template);

		md.setCloudifyInstalled(false);
		md.setInstallationDirectory(null);
		md.setMachineId(instance.getInstanceId());
		md.setPrivateAddress(instance.getPrivateIpAddress());
		md.setPublicAddress(instance.getPublicIpAddress());
		md.setRemoteUsername(template.getUsername());
		md.setRemotePassword(template.getPassword());
		String availabilityZone = instance.getPlacement().getAvailabilityZone();
		md.setLocationId(RegionUtils.convertAvailabilityZone2LocationId(availabilityZone));

		return md;
	}

	private Instance createEC2Instance(final PrivateEc2Template cfnTemplate) throws CloudProvisioningException,
			TimeoutException {

		InstanceProperties properties = cfnTemplate.getEC2Instance().getProperties();

		String availabilityZone =
				properties.getAvailabilityZone() == null ? null : properties.getAvailabilityZone().getValue();
		Placement placement = availabilityZone == null ? null : new Placement(availabilityZone);

		String imageId = properties.getImageId() == null ? null : properties.getImageId().getValue();
		String instanceType = properties.getInstanceType() == null ? null : properties.getInstanceType().getValue();
		String keyName = properties.getKeyName() == null ? null : properties.getKeyName().getValue();
		String privateIpAddress =
				properties.getPrivateIpAddress() == null ? null : properties.getPrivateIpAddress().getValue();
		List<String> securityGroupIds = properties.getSecurityGroupIdsAsString();
		List<String> securityGroups = properties.getSecurityGroupsAsString();

		String userData = null;
		if (properties.getUserData() != null) {
			StringBuilder sb = new StringBuilder();

			// Generate ENV script for the provisioned machine
			String script = this.generateCloudifyEnv();

			sb.append("#!/bin/bash\n");
			sb.append(script).append("\n");
			sb.append(properties.getUserData().getValue());

			userData = sb.toString();
			logger.fine("Instanciate ec2 with user data:\n" + userData);
			userData = StringUtils.newStringUtf8(Base64.encodeBase64(userData.getBytes()));
		}

		List<BlockDeviceMapping> blockDeviceMappings = null;
		AWSEC2Volume volumeConfig = null;
		if (properties.getVolumes() != null) {
			blockDeviceMappings = new ArrayList<BlockDeviceMapping>(properties.getVolumes().size());
			for (VolumeMapping volMapping : properties.getVolumes()) {
				volumeConfig = cfnTemplate.getEC2Volume(volMapping.getVolumeId().getValue());
				blockDeviceMappings.add(this.createBlockDeviceMapping(volMapping.getDevice().getValue(), volumeConfig));
			}
		}

		RunInstancesRequest runInstancesRequest = new RunInstancesRequest();
		runInstancesRequest.withPlacement(placement);
		runInstancesRequest.withImageId(imageId);
		runInstancesRequest.withInstanceType(instanceType);
		runInstancesRequest.withKeyName(keyName);
		runInstancesRequest.withPrivateIpAddress(privateIpAddress);
		runInstancesRequest.withSecurityGroupIds(securityGroupIds);
		runInstancesRequest.withSecurityGroups(securityGroups);
		runInstancesRequest.withMinCount(1);
		runInstancesRequest.withMaxCount(1);
		runInstancesRequest.withBlockDeviceMappings(blockDeviceMappings);
		runInstancesRequest.withUserData(userData);

		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("EC2::Instance request=" + runInstancesRequest);
		}

		RunInstancesResult runInstances = this.ec2.runInstances(runInstancesRequest);
		if (runInstances.getReservation().getInstances().size() != 1) {
			throw new CloudProvisioningException("Request runInstace fails (request=" + runInstancesRequest + ").");
		}

		return runInstances.getReservation().getInstances().get(0);
	}

	private BlockDeviceMapping createBlockDeviceMapping(final String device, final AWSEC2Volume volumeConfig)
			throws CloudProvisioningException {
		VolumeProperties volumeProperties = volumeConfig.getProperties();
		Integer iops = volumeProperties.getIops() == null ? null : volumeProperties.getIops();
		Integer size = volumeProperties.getSize();
		String snapshotId =
				volumeProperties.getSnapshotId() == null ? null : volumeProperties.getSnapshotId().getValue();
		String volumeType =
				volumeProperties.getVolumeType() == null ? null : volumeProperties.getVolumeType().getValue();

		EbsBlockDevice ebs = new EbsBlockDevice();
		ebs.setIops(iops);
		ebs.setSnapshotId(snapshotId);
		ebs.setVolumeSize(size);
		ebs.setVolumeType(volumeType);
		ebs.setDeleteOnTermination(true);

		BlockDeviceMapping mapping = new BlockDeviceMapping();
		mapping.setDeviceName(device);
		mapping.setEbs(ebs);
		return mapping;
	}

	private String generateCloudifyEnv() throws CloudProvisioningException {
		ProvisioningContext ctx = new ProvisioningContextAccess().getProvisioiningContext();
		ComputeTemplate template = new ComputeTemplate();
		// FIXME may not work on windows because of script language
		template.setScriptLanguage(ScriptLanguages.LINUX_SHELL);
		try {
			MachineDetails md = new MachineDetails();
			md.setLocationId(this.getManagementServersMachineDetails()[0].getLocationId());
			String script = ctx.createEnvironmentScript(md, template);
			return script;
		} catch (FileNotFoundException e) {
			logger.log(Level.SEVERE, "Couldn't find file: ", e.getMessage());
			throw new CloudProvisioningException(e);
		}
	}
}
