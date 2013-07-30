package org.cloudifysource.esc.driver.provisioning.privateEc2;

import org.apache.commons.lang.math.NumberUtils;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;

public class RegionUtils {

    /**
     * @param availabilityZone
     *            i.e us-east-1a, us-east-1b, ...
     * @return
     */
    public static Region convertAvailabilityZone2Region(String availabilityZone) {
        Region region;
        String regionStr = availabilityZone;
        if (!NumberUtils.isDigits(availabilityZone.substring(availabilityZone.length() - 1, availabilityZone.length()))) {
            regionStr = availabilityZone.substring(0, availabilityZone.length() - 1);

        }
        region = Region.getRegion(Regions.valueOf(regionStr.replaceAll("-", "_").toUpperCase()));
        return region;
    }

    /**
     * @param locationId
     *            i.e us-east-1
     * @return
     */
    public static Region convertLocationId2Region(String locationId) {
        locationId = locationId.replaceAll("-", "_").toUpperCase();
        Regions regionEnum = Regions.valueOf(locationId);
        Region region = Region.getRegion(regionEnum);
        return region;
    }

    /**
     * @param availabilityZone
     *            i.e us-east-1a, us-east-1b, ...
     * @return
     */
    public static String convertAvailabilityZone2LocationId(String availabilityZone) {
        String locationId = availabilityZone;
        if (!NumberUtils.isDigits(availabilityZone.substring(availabilityZone.length() - 1, availabilityZone.length()))) {
            locationId = availabilityZone.substring(0, availabilityZone.length() - 1);
        }
        return locationId;
    }
}
