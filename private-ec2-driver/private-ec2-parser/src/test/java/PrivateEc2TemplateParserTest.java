import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.cloudifysource.esc.driver.provisioning.privateEc2.parser.ParserUtils;
import org.cloudifysource.esc.driver.provisioning.privateEc2.parser.PrivateEc2ParserException;
import org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans.PrivateEc2Template;
import org.junit.Test;

public class PrivateEc2TemplateParserTest {

    @Test
    public void test() throws Exception {
        InputStream templateStream = ClassLoader.getSystemResourceAsStream("./cfn_templates/WordPress_Single_Instance_With_RDS.template");
        PrivateEc2Template template = ParserUtils.mapJson(PrivateEc2Template.class, templateStream);
        NiceOutput.toString(template);
        assertNotNull(template);
    }

    /**
     * @throws IOException
     * @throws PrivateEc2ParserException
     * 
     */
    @Test
    public void testTemplateWithEBS() throws IOException, PrivateEc2ParserException {
        InputStream templateStream = ClassLoader.getSystemResourceAsStream("./cfn_templates/EC2WithEBSSample.template");
        PrivateEc2Template template = ParserUtils.mapJson(PrivateEc2Template.class, templateStream);
        NiceOutput.toString(template);
        assertNotNull(template);
    }

    @Test
    public void testTemplateStaticWithVolume() throws IOException, PrivateEc2ParserException {
        InputStream templateStream = ClassLoader.getSystemResourceAsStream("./cfn_templates/volume.template");
        PrivateEc2Template template = ParserUtils.mapJson(PrivateEc2Template.class, templateStream);
        NiceOutput.toString(template);
        assertNotNull(template);
        assertNotNull((template.getEC2Instance()).getProperties());
        assertNotNull((template.getEC2Instance()).getProperties().getAvailabilityZone());
    }

    @Test
    public void testTemplateComplete() throws IOException, PrivateEc2ParserException {
        InputStream templateStream = ClassLoader.getSystemResourceAsStream("./cfn_templates/complete.template");
        ToStringBuilder.setDefaultStyle(ToStringStyle.SHORT_PREFIX_STYLE);
        PrivateEc2Template template = ParserUtils.mapJson(PrivateEc2Template.class, templateStream);
        NiceOutput.toString(template);
        assertNotNull(template);
    }

    @Test
    public void testTemplateWithRef() throws IOException, PrivateEc2ParserException {
        InputStream templateStream = ClassLoader.getSystemResourceAsStream("./cfn_templates/ref.template");
        ToStringBuilder.setDefaultStyle(ToStringStyle.SHORT_PREFIX_STYLE);
        PrivateEc2Template template = ParserUtils.mapJson(PrivateEc2Template.class, templateStream);
        NiceOutput.toString(template);
        assertNotNull(template);
    }

    @Test
    public void testTemplateWithTags() throws IOException, PrivateEc2ParserException {
        InputStream templateStream = ClassLoader.getSystemResourceAsStream("./cfn_templates/tags.template");
        PrivateEc2Template template = ParserUtils.mapJson(PrivateEc2Template.class, templateStream);
        NiceOutput.toString(template);
        assertNotNull(template);
        assertNotNull(template.getEC2Instance().getProperties().getTags());
        assertFalse(template.getEC2Instance().getProperties().getTags().isEmpty());
    }
}
