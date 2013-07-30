package org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;

public class PrivateEc2TemplateTest {

    PrivateEc2Template template = new PrivateEc2Template();

    @Test
    public void testGetEC2Volume() throws Exception {
        template.setResources(new ArrayList<AWSResource>());
        AWSEC2Volume vol1 = new AWSEC2Volume();
        vol1.setResourceName("volume1");
        template.getResources().add(vol1);

        AWSEC2Volume vol2 = new AWSEC2Volume();
        vol2.setResourceName("volume2");
        template.getResources().add(vol2);

        Assert.assertNotNull(template.getEC2Volume("volume2"));
        Assert.assertNotNull(template.getEC2Volume("volume1"));
        Assert.assertNull(template.getEC2Volume("volume3"));
        Assert.assertNotNull(template.getEC2Volume(null)); // Returns the first found
    }
}
