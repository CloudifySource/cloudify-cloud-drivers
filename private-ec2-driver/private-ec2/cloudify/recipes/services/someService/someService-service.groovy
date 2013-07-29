/*******************************************************************************
*
* Copyright (c) 2013 FastConnect
* a recipe that install sonar
*
*******************************************************************************/

service {
    name "someService"
    icon "someService-small-2.png"

    elastic true
    numInstances 1
    maxAllowedInstances 1

    lifecycle {
        startDetectionTimeoutSecs 900
        startDetection {
            true 
        }
        stopDetection { 
            false
        }
        locator {
            NO_PROCESS_LOCATORS
        }
    }
}

