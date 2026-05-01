package com.device.management.device_service;

import com.device.management.device_service.domain.DeviceEntity;
import com.device.management.device_service.domain.State;
import com.device.management.device_service.dto.request.DeviceRequest;
import com.device.management.device_service.dto.response.DeviceResponse;

import java.util.List;
import java.util.UUID;

import com.device.management.device_service.dto.request.DevicePatchRequest;

public final class TestFactory {

    public static final String DEFAULT_NAME = "iPhone 15 Pro";
    public static final String DEFAULT_BRAND = "Apple";
    public static final State DEFAULT_STATE = State.AVAILABLE;

    public static class DevicePage {
        public List<DeviceResponse> content;
        public int totalElements;
        public int totalPages;
        public int size;
        public int number;
    }

    // ─── DeviceRequest ─────────────────────────────────────────────────────

    public static DeviceRequest buildValidRequest() {
        return buildRequest(DEFAULT_NAME, DEFAULT_BRAND, DEFAULT_STATE);
    }

    public static DeviceRequest buildRequest(final String name,
                                             final String brand, final State state) {
        final DeviceRequest request = new DeviceRequest();
        request.setName(name);
        request.setBrand(brand);
        request.setState(state);
        return request;
    }

    // ─── DevicePatchRequest ────────────────────────────────────────────────

    public static DevicePatchRequest buildPatchRequestWithState(final State state) {
        final DevicePatchRequest request = new DevicePatchRequest();
        request.setState(state);
        return request;
    }

    public static DevicePatchRequest buildPatchRequestWithName(final String name) {
        final DevicePatchRequest request = new DevicePatchRequest();
        request.setName(name);
        return request;
    }

    public static DevicePatchRequest buildPatchRequestWithBrand(final String brand) {
        final DevicePatchRequest request = new DevicePatchRequest();
        request.setBrand(brand);
        return request;
    }

    // ─── DeviceEntity ──────────────────────────────────────────────────────

    public static DeviceEntity buildEntity(final String name, final String brand,
                                           final State state) {
        final DeviceEntity entity = new DeviceEntity();
        entity.setName(name);
        entity.setBrand(brand);
        entity.setState(state);
        return entity;
    }

    public static DeviceEntity buildEntity(final State state) {
        final DeviceEntity entity = new DeviceEntity();
        entity.setName(DEFAULT_NAME);
        entity.setBrand(DEFAULT_BRAND);
        entity.setState(state);
        return entity;
    }

    public static DeviceEntity buildEntity(final UUID deviceId, final State state) {
        final DeviceEntity entity = buildEntity(state);
        entity.setDeviceId(deviceId);
        return entity;
    }

    // ─── DeviceResponse ────────────────────────────────────────────────────

    public static DeviceResponse buildValidResponse(final UUID deviceId) {
        return buildResponse(deviceId, DEFAULT_NAME, DEFAULT_BRAND, DEFAULT_STATE);
    }

    public static DeviceResponse buildResponse(final UUID deviceId,
                                               final String name, final String brand, final State state) {
        final DeviceResponse response = new DeviceResponse();
        response.setDeviceId(deviceId);
        response.setName(name);
        response.setBrand(brand);
        response.setState(state);
        return response;
    }
}
