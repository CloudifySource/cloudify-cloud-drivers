import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DeleteTagsRequest;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeTagsRequest;
import com.amazonaws.services.ec2.model.DescribeTagsResult;
import com.amazonaws.services.ec2.model.DescribeVolumesRequest;
import com.amazonaws.services.ec2.model.DescribeVolumesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Image;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.TagDescription;
import com.amazonaws.services.ec2.model.Volume;

/**
 * Test class utility for manual testing of EC2 API.
 * 
 */
public class AWSRequestsHelperIT {

    private AmazonEC2 ec2;

    private AWSCredentials getAWSCredentials() throws IOException {
        Properties awscredentials = new Properties();
        awscredentials.load(new FileInputStream("./cloudify/clouds/privateEc2/privateEc2-cloud.properties"));
        String accessKey = ((String) awscredentials.get("accessKey")).replaceAll("\"", "");
        String secretKey = ((String) awscredentials.get("apiKey")).replaceAll("\"", "");
        AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
        return credentials;
    }

    @Before
    public void before() throws IOException {
        AWSCredentials credentials = this.getAWSCredentials();
        String endpoint = "ec2.us-east-1.amazonaws.com";
        ec2 = new AmazonEC2Client(credentials);
        ec2.setEndpoint(endpoint);
    }

    @Test
    public void testAmazonClientRegions() throws IOException {
        AWSCredentials credentials = getAWSCredentials();
        ec2 = new AmazonEC2Client(credentials);
        Region region = Region.getRegion(Regions.EU_WEST_1);
        ec2.setRegion(region);

        DescribeTagsResult describeTags = ec2.describeTags();

        System.out.println(describeTags);
    }

    @Test
    public void testTagRequest() {
        DescribeTagsRequest tagRequest = new DescribeTagsRequest();
        tagRequest.withFilters(new Filter("resource-type", Arrays.asList("volume")));
        // tagRequest.withFilters(new Filter("value", Arrays.asList("NewVolume")));
        DescribeTagsResult describeTags = ec2.describeTags(tagRequest);
        List<TagDescription> tags = describeTags.getTags();
        for (TagDescription tag : tags) {
            System.out.println(tag);
        }
    }

    @Test
    public void removeVolumeTags() {
        DescribeTagsResult describeTags = ec2.describeTags();
        List<TagDescription> tags = describeTags.getTags();

        List<String> resourceIds = new ArrayList<String>();
        List<Tag> toRemove = new ArrayList<Tag>();
        for (TagDescription td : tags) {
            if (td.getResourceId().startsWith("vol-")) {
                resourceIds.add(td.getResourceId());
                System.out.println("remove: " + td);
            }
        }

        DeleteTagsRequest request = new DeleteTagsRequest();
        request.setResources(resourceIds);
        ec2.deleteTags(request);

    }

    @Test
    public void testVolumeRequest() {
        DescribeVolumesRequest volumeRequest = new DescribeVolumesRequest();
        volumeRequest.setVolumeIds(Arrays.asList((String) null));
        DescribeVolumesResult describeVolumes = ec2.describeVolumes(volumeRequest);
        List<Volume> volumes = describeVolumes.getVolumes();
        for (Volume volume : volumes) {
            System.out.println(volume);
        }
    }

    @Test
    public void testInstanceDescriptionRequest() {
        DescribeInstancesRequest describeInstance = new DescribeInstancesRequest();
        describeInstance.withFilters(new Filter("private-ip-address", Arrays.asList("10.36.157.75")));
        DescribeInstancesResult describeInstances = ec2.describeInstances(describeInstance);
        System.out.println(describeInstances);
    }

    @Test
    public void testTagRequestStartWith() {
        DescribeTagsRequest tagRequest = new DescribeTagsRequest();
        tagRequest.withFilters(new Filter("resource-type", Arrays.asList("volume")));
        tagRequest.withFilters(new Filter("key", Arrays.asList("ApplicationName", "Name")));
        tagRequest.withFilters(new Filter("value", Arrays.asList("someService*", "sampleApplication")));
        // tagRequest.withFilters(new Filter("key", Arrays.asList("Name")));
        // tagRequest.withFilters(new Filter("value", Arrays.asList("someService")));
        DescribeTagsResult describeTags = ec2.describeTags(tagRequest);
        List<TagDescription> tags = describeTags.getTags();
        for (TagDescription tag : tags) {
            System.out.println(tag);
        }
    }

    @Test
    public void testDescribeVolumeRequest() {
        DescribeVolumesRequest request = new DescribeVolumesRequest();
        request.withFilters(new Filter("status", Arrays.asList("available")),
                new Filter("tag-key", Arrays.asList("_Instance_")));

        DescribeVolumesResult describeVolumes = ec2.describeVolumes(request);

        for (Volume volume : describeVolumes.getVolumes()) {
            System.out.println(volume);
        }
    }

    @Test
    public void testDescribeImageRequest() {
        DescribeImagesRequest dir = new DescribeImagesRequest();
        dir.setImageIds(Arrays.asList("ami-d9d6a6b0", "ami-00dd3069", "ami-0edd3067"));
        DescribeImagesResult describeImages = ec2.describeImages(dir);
        for (Image image : describeImages.getImages()) {
            System.out.println(image.getImageId() + " -> " + image.getPlatform() + " - " + image);
        }
    }
}
