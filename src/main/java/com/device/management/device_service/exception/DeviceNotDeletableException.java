package com.device.management.device_service.exception;

import java.util.UUID;

public class DeviceNotDeletableException extends RuntimeException {

    public DeviceNotDeletableException(final UUID deviceId) {
        super("Device with id: " + deviceId + " is in use and cannot be deleted");
    }
}
