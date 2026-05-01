package com.device.management.device_service.exception;

import java.util.UUID;

public class DeviceNotUpdatableException extends RuntimeException {

    public DeviceNotUpdatableException(final UUID deviceId) {
        super("Device with id: " + deviceId + " is in use, name and brand cannot be updated");
    }
}
