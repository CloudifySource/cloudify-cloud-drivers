package org.cloudifysource.esc.driver.provisioning.privateEc2;

/**
 * http://docs.aws.amazon.com/AWSEC2/latest/APIReference/ApiReference-ItemType-InstanceStateType.html
 * 
 */
public enum InstanceStateType {
    PENDING("pending", 0),
    RUNNING("running", 16),
    SHUTTING_DOWN("shutting-down", 32),
    TERMINATED("terminated", 48),
    STOPPING("stopping", 64),
    STOPPED("stopped", 80);

    private String name;
    private int code;

    InstanceStateType(String name, int code) {
        this.name = name;
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public int getCode() {
        return code;
    }

    public static InstanceStateType valueOf(int code) {
        switch (code) {
        case 0:
            return PENDING;
        case 16:
            return RUNNING;
        case 32:
            return SHUTTING_DOWN;
        case 48:
            return TERMINATED;
        case 64:
            return STOPPING;
        case 80:
        default:
            return STOPPED;
        }
    }

}
