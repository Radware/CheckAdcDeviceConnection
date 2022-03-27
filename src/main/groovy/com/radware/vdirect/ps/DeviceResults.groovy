package com.radware.vdirect.ps

class DeviceResults {
    DeviceResults(String deviceName, String reason = ""){

        this.deviceName = deviceName
        this.reason = reason
    }
    String deviceName
    String reason
}
