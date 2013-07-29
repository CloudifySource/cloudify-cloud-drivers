package org.cloudifysource.esc.driver.provisioning.privateEc2;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;
import org.cloudifysource.esc.driver.provisioning.MachineDetails;
import org.cloudifysource.esc.driver.provisioning.ProvisioningContextAccess;
import org.cloudifysource.esc.driver.provisioning.ProvisioningContextImpl;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Manual test class for the driver.
 * 
 */
public class PrivateEC2CloudifyDriverIT {
    private static final Logger logger = Logger.getLogger(PrivateEC2CloudifyDriverIT.class.getName());

    private PrivateEC2CloudifyDriver driver;

    private Cloud cloud;

    @BeforeClass
    public static void beforeClass() {
        Logger logger = Logger.getLogger(PrivateEC2CloudifyDriverIT.class.getName());
        logger.setLevel(Level.FINEST);
        Handler[] handlers = logger.getParent().getHandlers();
        for (Handler handler : handlers) {
            handler.setLevel(Level.ALL);
        }
    }

    @Before
    public void before() throws CloudProvisioningException, DSLException {
        File file = new File("cloudify/clouds/privateEc2"); // Path to '*-cloud.groovy'
        logger.info(file.getAbsolutePath());
        cloud = ServiceReader.readCloudFromDirectory(file.getAbsolutePath(), null);
        driver = new PrivateEC2CloudifyDriver();
    }

    /**
     * Requires a Management Machine up
     */
    @Test
    public void testStartMachine() throws Exception {
        String cloudTemplateName = "CFN_TEMPLATE";

        ProvisioningContextImpl ctx = new ProvisioningContextImpl();
        ProvisioningContextAccess.setCurrentProvisioingContext(ctx);
        ctx.getInstallationDetailsBuilder().setCloud(this.cloud);
        ctx.getInstallationDetailsBuilder().setTemplate(this.cloud.getCloudCompute().getTemplates().get(cloudTemplateName));

        driver.setCustomDataFile(new File("./cloudify/cfn-templates/sampleApplication"));
        driver.setConfig(cloud, cloudTemplateName, true, "someService");
        MachineDetails md = driver.startMachine(null, 60, TimeUnit.MINUTES);

        assertNotNull(md.getPrivateAddress());
        assertNotNull(md.getPublicAddress());
    }

    @Test
    public void testStopMachine() throws Exception {
        driver.setCustomDataFile(new File("./src/test/resources/cfn_templates/"));
        driver.setConfig(cloud, "CFN_TEMPLATE", true, "tags");
        driver.stopMachine("10.36.174.213", 60, TimeUnit.MINUTES);
    }

    @Test
    public void testStopMachineWithAmazonSDK() throws Exception {
        driver.setCustomDataFile(new File("./src/test/resources/cfn_templates/"));
        driver.setConfig(cloud, cloud.getConfiguration().getManagementMachineTemplate(), true, "static-with-volume");
        driver.stopMachine("10.48.110.158", 1, TimeUnit.HOURS);
    }
}
