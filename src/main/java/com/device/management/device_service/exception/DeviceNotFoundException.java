package com.device.management.device_service.exception;

import java.util.UUID;

public class DeviceNotFoundException extends RuntimeException {

    public DeviceNotFoundException(final UUID deviceId){
        super("Device not found with id: " + deviceId);
    }
}
