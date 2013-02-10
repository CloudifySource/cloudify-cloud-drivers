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
*******************************************************************************/
package org.cloudifysource.esc.driver.provisioning.smartcloud;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.dsl.cloud.CloudTemplate;
import org.cloudifysource.esc.driver.provisioning.CloudDriverSupport;
import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;
import org.cloudifysource.esc.driver.provisioning.MachineDetails;
import org.cloudifysource.esc.driver.provisioning.ProvisioningDriver;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.api.client.filter.LoggingFilter;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**************
 * A custom cloud driver for IBM SmartCloud. 
 * 
 * 
 * @author aharon
 * @since 2.3
 * 
 */
public class SmartCloudDriver extends CloudDriverSupport implements ProvisioningDriver {

	private static final int MILLIS_IN_SECOND = 1000;
	private static final String MACHINE_STATUS_ACTIVE = "5";
	private static final int INTERNAL_SERVER_ERROR = 500;
	private static final int SERVER_POLLING_INTERVAL_MILLIS = 10 * 1000; // 10 seconds
	private static final int DEFAULT_SHUTDOWN_TIMEOUT_MILLIS = 10 * 60 * 1000; // 10 minutes
	private static final int DEFAULT_TIMEOUT_AFTER_CLOUD_INTERNAL_ERROR = 30 * 1000; // 30 seconds
	private static final String smartcloud_WIRE_LOG = "smartcloud.wireLog";
	private static final String smartcloud_KEY_PAIR = "smartcloud.keyPair";
	private static final String smartcloud_smartcloud_ENDPOINT = "smartcloud.endpoint";
	private static final String smartcloud_LOCATION = "smartcloud.location";
	private static final String STARTING_THROTTLING = "The cloud reported an Internal Server Error (status 500)."
			+ " Requests for new machines will be suspended for "
			+ DEFAULT_TIMEOUT_AFTER_CLOUD_INTERNAL_ERROR / MILLIS_IN_SECOND + " seconds";
	private static final String RUNNING_THROTTLING = "Requests for new machines are currently suspended";

	private final XPath xpath = XPathFactory.newInstance().newXPath();

	private final Client client;

	private long throttlingTimeout = -1;
	private String serverNamePrefix;
	private String location;
	private String endpoint;
	private WebResource service;
	private final DocumentBuilderFactory dbf;
	private final Object xmlFactoryMutex = new Object();

	/************
	 * Constructor.
	 * 
	 * @throws ParserConfigurationException
	 */
	public SmartCloudDriver() {
		dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(false);

		final ClientConfig config = new DefaultClientConfig();
		this.client = Client.create(config);

	}

	private DocumentBuilder createDocumentBuilder() {
		synchronized (xmlFactoryMutex) {
			// Document builder is not guaranteed to be thread sage
			try {
				// Document builders are not thread safe
				return dbf.newDocumentBuilder();
			} catch (final ParserConfigurationException e) {
				throw new IllegalStateException("Failed to set up XML Parser", e);
			}
		}

	}
	
	/**
	 *  Called when the service that this provisioning implementation is responsible for scaling is undeployed.
	 *  The implementation is expected to release/close all relevant resources, such as thread pools, sockets, files, etc.
	 */
	@Override
	public void close() {
	}

	/**
	 * @Return the name of this cloud.
	 */
	@Override
	public String getCloudName() {
		return "smartcloud";
	}

	/**
	 * Called once on startup of the cloud driver, passing the cloud configuration to it.
	 * 
	 * @param cloud - The cloud configuration for this driver
	 * @param templateName - The template required for this cloud driver.
	 * @param management - true if this driver will launch management machines, false otherwise.
	 * @param serviceName - The name of the service that is planned to be installed on this machine. Could be null if this is a management machine hosting more than one service
	 */
	@Override
	public void setConfig(final Cloud cloud, final String templateName,
			final boolean management, final String serviceName) {
		super.setConfig(cloud, templateName, management, serviceName);
		if (this.management) {
			this.serverNamePrefix = this.cloud.getProvider().getManagementGroup();
		} else {
			this.serverNamePrefix = this.cloud.getProvider().getMachineNamePrefix();
		}

		this.location = (String) this.cloud.getCustom().get(smartcloud_LOCATION);
		if (location == null) {
			throw new IllegalArgumentException("Custom field '" + smartcloud_LOCATION + "' must be set");
		}


		this.endpoint = (String) this.cloud.getCustom().get(smartcloud_smartcloud_ENDPOINT);
		if (this.endpoint == null) {
			throw new IllegalArgumentException("Custom field '" + smartcloud_smartcloud_ENDPOINT + "' must be set");
		}

		this.service = client.resource(this.endpoint);
		this.service.addFilter(new HTTPBasicAuthFilter(this.cloud.getUser().getUser(),this.cloud.getUser().getApiKey()));

		final String wireLog = (String) this.cloud.getCustom().get(smartcloud_WIRE_LOG);
		if (wireLog != null) {
			if (Boolean.parseBoolean(wireLog)) {
				this.client.addFilter(new LoggingFilter(logger));
			}
		}

	}

	/**
	 * Starts an additional machine on the cloud , on the specific location, to scale out this specific service.
	 * In case of an error while provisioning the machine, any allocated resources should be freed before throwing a CloudProvisioningException or TimeoutException to the caller.
	 * 
	 * @param locationId - the location to allocate the machine to.
	 * @param duration - Time duration to wait for the instance.
	 * @param unit - Time unit to wait for the instance.
	 * 
	 * @return The details of the started instance.
	 * 
	 * @throws java.util.concurrent.TimeoutException - In case the instance was not started in the allotted time.
	 * @throws org.cloudifysource.esc.driver.provisioning.CloudProvisioningException - If a problem was encountered while starting the machine.
	 */
	@Override
	public MachineDetails startMachine(final String locationId, final long duration, final TimeUnit unit)
			throws TimeoutException, CloudProvisioningException {

		if (isThrottling()) {
			throw new CloudProvisioningException(RUNNING_THROTTLING);
		}

		final long endTime = System.currentTimeMillis() + unit.toMillis(duration);

		MachineDetails md;
		try {
			md = newServer(endTime, this.template);
		} catch (final Exception e) {
			if (e instanceof UniformInterfaceException
					&& ((UniformInterfaceException) e).getResponse().getStatus() == INTERNAL_SERVER_ERROR) {
				throttlingTimeout = calcEndTimeInMillis(DEFAULT_TIMEOUT_AFTER_CLOUD_INTERNAL_ERROR,
						TimeUnit.MILLISECONDS);
				throw new CloudProvisioningException(STARTING_THROTTLING, e);
			} else {
				throw new CloudProvisioningException(e);
			}
		}
		return md;
	}

	private long calcEndTimeInMillis(final long duration, final TimeUnit unit) {
		return System.currentTimeMillis() + unit.toMillis(duration);
	}

	/**
	 * Start the management machines for this cluster. 
	 * This method is called once by the cloud administrator when bootstrapping a new cluster.
	 * 
	 * @param duration - timeout duration.
	 * @param unit - timeout unit.
	 * 
	 * @return The created machine details.
	 * 
	 * @throws java.util.concurrent.TimeoutException - If creating the new machines exceeded the given timeout.
	 * @throws org.cloudifysource.esc.driver.provisioning.CloudProvisioningException - If the machines needed for management could not be provisioned.
	 */
	@Override
	public MachineDetails[] startManagementMachines(final long duration, final TimeUnit unit)
			throws TimeoutException, CloudProvisioningException {
		final long endTime = calcEndTimeInMillis(duration, unit);

		final int numOfManagementMachines = cloud.getProvider().getNumberOfManagementMachines();

		// thread pool - one per machine
		final ExecutorService executor =
				Executors.newFixedThreadPool(cloud.getProvider().getNumberOfManagementMachines());

		try {
			return doStartManagement(endTime, numOfManagementMachines, executor);
		} finally {
			executor.shutdown();
		}
	}

	private MachineDetails[] doStartManagement(final long endTime, 
			final int numOfManagementMachines, final ExecutorService executor)
			throws CloudProvisioningException {

		// launch machine on a thread
		final List<Future<MachineDetails>> list = new ArrayList<Future<MachineDetails>>(numOfManagementMachines);
		for (int i = 0; i < numOfManagementMachines; ++i) {
			final Future<MachineDetails> task = executor.submit(new Callable<MachineDetails>() {

				@Override
				public MachineDetails call()
						throws Exception {

					final MachineDetails md = newServer(endTime, template);
					return md;

				}

			});
			list.add(task);

		}

		// get the machines
		Exception firstException = null;
		final List<MachineDetails> machines = new ArrayList<MachineDetails>(numOfManagementMachines);
		for (final Future<MachineDetails> future : list) {
			try {
				machines.add(future.get());
			} catch (final Exception e) {
				if (firstException == null) {
					firstException = e;
				}
			}
		}

		if (firstException == null) {
			return machines.toArray(new MachineDetails[machines.size()]);
		} else {
			// in case of an exception, clear the machines
			logger.warning("Provisioning of management machines failed, the following node will be shut down: "
					+ machines);
			for (final MachineDetails machineDetails : machines) {
				try {
					this.terminateServer(machineDetails.getMachineId(),endTime);
				} catch (final Exception e) {
					logger.log(Level.SEVERE,
							"While shutting down machine after provisioning of management machines failed, "
									+ "shutdown of node: " + machineDetails.getMachineId()
									+ " failed. This machine may be leaking. Error was: " + e.getMessage(), e);
				}
			}

			throw new CloudProvisioningException(
					"Failed to launch management machines: " + firstException.getMessage(), firstException);
		}
	}

	/**
	 * Stops a specific machine for scaling in or shutting down a specific service.
	 * 
	 * @param ip - host-name/IP of the machine to shut down.
	 * @param duration - time to wait for the shutdown operation.
	 * @param unit - time unit for the shutdown operations
	 * 
	 * @return true if the operation succeeded, false otherwise.
	 * 
	 * @throws java.lang.InterruptedException - If the operation was interrupted.
	 * @throws java.util.concurrent.TimeoutException - If the operation exceeded the given timeout.
	 * @throws org.cloudifysource.esc.driver.provisioning.CloudProvisioningException - If the operation encountered an error.
	 */
	@Override
	public boolean stopMachine(final String ip, final long duration, final TimeUnit unit)
			throws InterruptedException, TimeoutException, CloudProvisioningException {
		final long endTime = calcEndTimeInMillis(duration, unit);

		if (isStopRequestRecent(ip)) {
			return false;
		}

		try {
			terminateServerByIp(ip, endTime);
			return true;
		} catch (final Exception e) {
			throw new CloudProvisioningException(e);
		}
	}

	/**
	 * Stops the management machines.
	 * 
	 * @throws java.util.concurrent.TimeoutException - in case the stop operation exceeded the given timeout.
	 * @throws org.cloudifysource.esc.driver.provisioning.CloudProvisioningException - If the stop operation failed.
	 */
	@Override
	public void stopManagementMachines()
			throws TimeoutException, CloudProvisioningException {

		final long endTime = calcEndTimeInMillis(DEFAULT_SHUTDOWN_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
		List<Node> nodes;
		try {
			nodes = listServers();
		} catch (final SmartCloudException e) {
			throw new CloudProvisioningException(e);
		}

		final List<String> ids = new LinkedList<String>();
		for (final Node node : nodes) {
			if (node.getName().startsWith(this.serverNamePrefix)) {
				try {
					ids.add(node.getId());

				} catch (final Exception e) {
					throw new CloudProvisioningException(e);
				}
			}
		}

		try {
			terminateServers(ids, endTime);
		} catch (final TimeoutException e) {
			throw e;
		} catch (final Exception e) {
			throw new CloudProvisioningException("Failed to shut down managememnt machines", e);
		}
	}

	private Node getNode(final String nodeId)
			throws SmartCloudException {
		final Node node = new Node();
		String response = "";
		try {
			response =	service.path("instances").accept(MediaType.APPLICATION_XML).get(String.class);

			final DocumentBuilder documentBuilder = createDocumentBuilder();
			final Document xmlDoc = documentBuilder.parse(new InputSource(new StringReader(response)));

			node.setId(xpath.evaluate("//Instance[ID='"+nodeId+"']/ID", xmlDoc));
			node.setStatus(xpath.evaluate("//Instance[ID='"+nodeId+"']/Status", xmlDoc));
			node.setName(xpath.evaluate("//Instance[ID='"+nodeId+"']/Name", xmlDoc));
			//one IP return 
			//node.setPrivateIp(xpath.evaluate("//Instance[ID='"+nodeId+"']/IP", xmlDoc));
			node.setPublicIp(xpath.evaluate("//Instance[ID='"+nodeId+"']/IP", xmlDoc));

			logger.finer("In getNode method, looking for server id : "+nodeId);
			logger.finer("In getNode method, server id is : "+nodeId+" its status is: "+ node.getStatus()+" --> "+ getNodeStatus(node.getStatus()));
			if (node.getId().equals(""))
			{
				logger.finer("In getNode method, no server id found : "+nodeId+ " throwing SmartCloudException ");
				throw new SmartCloudException("No node found");
			}
		} catch (final XPathExpressionException e) {
			throw new SmartCloudException("Failed to parse XML Response from server. Response was: " + response
					+ ", Error was: " + e.getMessage(), e);
		} catch (final SAXException e) {
			throw new SmartCloudException("Failed to parse XML Response from server. Response was: " + response
					+ ", Error was: " + e.getMessage(), e);
		} catch (final IOException e) {
			throw new SmartCloudException("Failed to send request to server. Response was: " + response
					+ ", Error was: " + e.getMessage(), e);
		} catch (UniformInterfaceException e) {
			throw new SmartCloudException("Failed on get for server with node id " + nodeId + ". Response was: " + response
					+ ", Error was: " + e.getMessage(), e);
		}

		return node;

	}

	List<Node> listServers()
			throws SmartCloudException {
		logger.finer("In listServers method berfore the getNode loop");
		final List<String> ids = listServerIds();
		final List<Node> nodes = new ArrayList<Node>();
	
		for (final String id : ids) {
			try {
			Node node = getNode(id);
			nodes.add(node);
			} catch (SmartCloudException e) {
				//Do nothing.
			}
		}

		return nodes;
	}


	private List<String> listServerIds()
			throws SmartCloudException {

		String response = null;
		try {
			response =	service.path("instances").accept(MediaType.APPLICATION_XML).get(String.class);
			final DocumentBuilder documentBuilder = createDocumentBuilder();
			final Document xmlDoc = documentBuilder.parse(new InputSource(new StringReader(response)));

			final NodeList idNodes = (NodeList) xpath.evaluate("//Instance/ID", xmlDoc,XPathConstants.NODESET);
			final int howmany = idNodes.getLength();
			final List<String> ids = new ArrayList<String>(howmany);
			for (int i = 0; i < howmany; i++) {
				ids.add(idNodes.item(i).getTextContent());

			}
			return ids;

		} catch (final UniformInterfaceException e) {
			final String responseEntity = e.getResponse().getEntity(String.class);
			throw new SmartCloudException(e + " Response entity: " + responseEntity, e);

		} catch (final SAXException e) {
			throw new SmartCloudException("Failed to parse XML Response from server. Response was: " + response
					+ ", Error was: " + e.getMessage(), e);
		} catch (final XPathException e) {
			throw new SmartCloudException("Failed to parse XML Response from server. Response was: " + response
					+ ", Error was: " + e.getMessage(), e);
		} catch (final IOException e) {
			throw new SmartCloudException("Failed to send request to server. Response was: " + response
					+ ", Error was: " + e.getMessage(), e);
		}
	}

	private void terminateServerByIp(final String serverIp, final long endTime)
			throws Exception {
		logger.finer("Terminating machine with IP " + serverIp);
		final Node node = getNodeByIp(serverIp);
		if (node == null) {
			throw new IllegalArgumentException("Could not find a server with IP: " + serverIp);
		}
		logger.finer("Terminating node with the following detailes: " + node.toString());
		terminateServer(node.getId(), endTime);
	}

	private Node getNodeByIp(final String serverIp)
			throws SmartCloudException {
		final List<Node> nodes = listServers();
		for (final Node node : nodes) {
			if ((node.getPrivateIp() != null && node.getPrivateIp().equalsIgnoreCase(serverIp))
					|| (node.getPublicIp() != null && node.getPublicIp().equalsIgnoreCase(serverIp))) {
				logger.finer("Server with IP " + serverIp + " Matches node: " + node.toString());
				return node;
			}
		}

		return null;
	}

	private void terminateServer(final String serverId, final long endTime)
			throws Exception {
		terminateServers(Arrays.asList(serverId), endTime);
	}

	private void terminateServers(final List<String> serverIds, final long endTime)
			throws Exception {

		// detach public ip and delete the servers
		for (final String serverId : serverIds) {
			try {
				logger.finer("In terminateServers deletes from cloud: "+serverId);
				service.path("instances/"+serverId).accept(MediaType.APPLICATION_XML).delete();
			} catch (final UniformInterfaceException e) {
				final String responseEntity = e.getResponse().getEntity(String.class);
				throw new IllegalArgumentException(e + " Response entity: " + responseEntity);
			}

		}

		int successCounter = 0;

		// wait for all servers to die
		for (final String serverId : serverIds) {
			while (System.currentTimeMillis() < endTime) {
				try {
					logger.finer("In terminateServers while loop, shutting down server id: "+serverId);
					this.getNode(serverId);

				} catch (final SmartCloudException e) {
					logger.finer("In terminateServers method, getting SmartCloudException checking if No node found");
					if (e.getMessage().equals("No node found"))
					{
							++successCounter;
							logger.finer("In terminateServers method, No node found successCounter= "+successCounter);
							break;
					}
					throw e;
				}
				Thread.sleep(SERVER_POLLING_INTERVAL_MILLIS);
			}

		}

		if (successCounter == serverIds.size()) {
			logger.finer("In terminateServers method successCounter == serverIds.size() return");
			return;
		}

		throw new TimeoutException("Nodes " + serverIds + " did not shut down in the required time");

	}

	/**
	 * Creates server. Block until complete. Returns id
	 * 
	 * @param name the server name
	 * @param timeout the timeout in seconds
	 * @param serverTemplate the cloud template to use for this server
	 * @return the server id
	 */
	private MachineDetails newServer(final long endTime, final CloudTemplate serverTemplate)
			throws Exception {

		final String serverId = createServer(serverTemplate);

		try {
			final MachineDetails md = new MachineDetails();
			logger.finer("In the newServer --> starting the waitForServerToReachStatus method"); 
			// wait until complete
			waitForServerToReachStatus(md, endTime, serverId, MACHINE_STATUS_ACTIVE);

			// if here, we have a node with a private and public ip.
			final Node node = this.getNode(serverId);

			md.setPublicAddress(node.getPublicIp());
			md.setMachineId(serverId);
			md.setAgentRunning(false);
			md.setCloudifyInstalled(false);
			md.setInstallationDirectory(serverTemplate.getRemoteDirectory());

			md.setRemoteUsername(serverTemplate.getUsername());

			return md;
		} catch (final Exception e) {
			logger.log(Level.WARNING, "server: " + serverId + " failed to start up correctly. "
					+ "Shutting it down. Error was: " + e.getMessage(), e);
			try {
				terminateServer(serverId,  endTime);
			} catch (final Exception e2) {
				logger.log(Level.WARNING,
						"Error while shutting down failed machine: " + serverId + ". Error was: " + e.getMessage()
								+ ".It may be leaking.", e);
			}
			throw e;
		}

	}

	private String createServer(final CloudTemplate serverTemplate)
			throws SmartCloudException {
		final String serverName = this.serverNamePrefix + System.currentTimeMillis();
		//final String securityGroup = getCustomTemplateValue(serverTemplate, smartcloud_SECURITYGROUP, null, false);
		final String keyPairName = getCustomTemplateValue(serverTemplate, smartcloud_KEY_PAIR, null, false);

		// Start the machine!
		MultivaluedMap formData = new MultivaluedMapImpl();
		formData.add("name", serverName);
		formData.add("imageID", serverTemplate.getImageId());
		formData.add("instanceType", serverTemplate.getHardwareId());
		formData.add("location", location);
		formData.add("publicKey", keyPairName);
		String serverBootResponse = null;
		try {
			serverBootResponse =
					service.path("instances")
							.accept(MediaType.APPLICATION_XML).post(String.class, formData);
		} catch (final UniformInterfaceException e) {
			final String responseEntity = e.getResponse().getEntity(String.class);
			throw new SmartCloudException(e + " Response entity: " + responseEntity, e);
		}

		String status=null;
		try {
			// if we are here, the machine started!
			final DocumentBuilder documentBuilder = createDocumentBuilder();
			final Document doc = documentBuilder.parse(new InputSource(new StringReader(serverBootResponse)));

			status = xpath.evaluate("//Instance/Status/text()", doc);
			if (status !=null && status.length()>0)
			{
					if (Integer.valueOf(status)>5 || Integer.valueOf(status)<0) {
						throw new IllegalStateException("Expected server status of 0-5, got: " + status);
				}
			}

			final String serverId = xpath.evaluate("//Instance/ID/text()", doc);
			return serverId;
		} catch (final NumberFormatException e) {
			throw new SmartCloudException("Return status expecting number but got: " + status
					+ serverBootResponse + ", Error was: " + e.getMessage(), e);
			}
			catch (final XPathExpressionException e) {
			throw new SmartCloudException("Failed to parse XML Response from server. Response was: "
					+ serverBootResponse + ", Error was: " + e.getMessage(), e);
		} catch (final SAXException e) {
			throw new SmartCloudException("Failed to parse XML Response from server. Response was: "
					+ serverBootResponse + ", Error was: " + e.getMessage(), e);
		} catch (final IOException e) {
			throw new SmartCloudException("Failed to send request to server. Response was: " + serverBootResponse
					+ ", Error was: " + e.getMessage(), e);
		}
	}

	private String getCustomTemplateValue(final CloudTemplate serverTemplate, final String key,
			final String defaultValue, final boolean allowNull) {
		final String value = (String) serverTemplate.getOptions().get(key);
		if (value == null) {
			if (allowNull) {
				return defaultValue;
			} else {
				throw new IllegalArgumentException("Template option '" + key + "' must be set");
			}
		} else {
			return value;
		}

	}

	private void waitForServerToReachStatus(final MachineDetails md, final long endTime, final String serverId,	final String status)
			throws SmartCloudException, TimeoutException, InterruptedException {

		final String respone = null;
		while (true) {
			logger.finer("In the loop of waitForServerToReachStatus --> starting the getNode method"); 

			final Node node = this.getNode(serverId);

			final String currentStatus = node.getStatus().toLowerCase();

			if (currentStatus.equalsIgnoreCase(status)) {

				md.setPrivateAddress(node.getPrivateIp());
				break;
			} else {
				if (currentStatus.contains("error")) {
					throw new SmartCloudException("Server provisioning failed. Node ID: " + node.getId() + ", status: "
							+ node.getStatus());
				}

			}

			if (System.currentTimeMillis() > endTime) {
				throw new TimeoutException("timeout creating server. last status:" + respone);
			}

			Thread.sleep(SERVER_POLLING_INTERVAL_MILLIS);
		}

	}


	/**
	 * Checks if throttling is now activated, to avoid overloading the cloud.
	 * 
	 * @return True if throttling is activate, false otherwise
	 */
	public boolean isThrottling() {
		boolean throttling = false;
		if (throttlingTimeout > 0 && throttlingTimeout - System.currentTimeMillis() > 0) {
			throttling = true;
		}

		return throttling;
	}
	
	String getNodeStatus(String status)
	{
		int statusNumber = Integer.valueOf(status);
		switch (statusNumber)
	      {
	        //the choices go here - print the details
	        case 0:
	      	  	return "New";
	        case 1:
	        	return "Provisioning";
	        case 2:
	        	return "Failed";
	        case 3:
	        	return "Removed";
	        case 4: 
	        	return "Rejected";
	        case 5:
	      	  	return "Active";
	        case 6:
	        	return "Unknown";
	        case 7:
	        	return "Deprovisioning";
	        case 8:
	        	return "Restarting";
	        case 9: 
	        	return "Starting";
	        case 10:
	      	  	return "Stopping";
	        case 11:
	        	return "Stopped";
	        case 12:
	        	return "Deprovisioning pending";
	        case 13:
	        	return "Restart pending";
	        case 14: 
	        	return "Attaching";
	        case 15: 
	        	return "Detaching";
	        default:
	        	return "Status Undifined: "+statusNumber;
	      } 
	}
}
