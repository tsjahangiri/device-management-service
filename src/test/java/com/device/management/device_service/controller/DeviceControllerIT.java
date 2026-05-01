package com.device.management.device_service.controller;

import com.device.management.device_service.BaseIntegrationTest;
import com.device.management.device_service.TestFactory;
import com.device.management.device_service.domain.State;
import com.device.management.device_service.dto.request.DevicePatchRequest;
import com.device.management.device_service.dto.request.DeviceRequest;
import com.device.management.device_service.dto.response.DeviceResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static com.device.management.device_service.TestFactory.*;
import static org.assertj.core.api.Assertions.assertThat;

public class DeviceControllerIT extends BaseIntegrationTest {

    private static final String UPDATED_NAME = "iPhone 16 Pro";
    private static final String SAMSUNG_BRAND = "Samsung";
    private static final String MACBOOK_NAME = "MacBook Pro";
    private static final String GALAXY_NAME = "Galaxy S24";

    // ─── CREATE DEVICE ────────────────────────────────────────────────────────────

    @Test
    void createDevice_validRequest_returns201() {
        final DeviceRequest request = buildValidRequest();

        final ResponseEntity<DeviceResponse> response = restTemplate
                .postForEntity(baseUrl(), request, DeviceResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDeviceId()).isNotNull();
        assertThat(response.getBody().getName()).isEqualTo(DEFAULT_NAME);
        assertThat(response.getBody().getBrand()).isEqualTo(DEFAULT_BRAND);
        assertThat(response.getBody().getState()).isEqualTo(DEFAULT_STATE);
    }

    @Test
    void createDevice_missingName_returns400() {
        final DeviceRequest request = buildRequest(null, DEFAULT_BRAND, DEFAULT_STATE);

        final ResponseEntity<Void> response = restTemplate
                .postForEntity(baseUrl(), request, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void createDevice_missingBrand_returns400() {
        final DeviceRequest request = buildRequest(DEFAULT_NAME, null, DEFAULT_STATE);

        final ResponseEntity<Void> response = restTemplate
                .postForEntity(baseUrl(), request, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void createDevice_missingState_returns400() {
        final DeviceRequest request = buildRequest(DEFAULT_NAME, DEFAULT_BRAND, null);

        final ResponseEntity<Void> response = restTemplate
                .postForEntity(baseUrl(), request, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ─── GET SINGLE DEVICE ────────────────────────────────────────────────────────

    @Test
    void getDevice_existingDevice_returns200() {
        final UUID deviceId = createDevice(MACBOOK_NAME, DEFAULT_BRAND, DEFAULT_STATE);

        final ResponseEntity<DeviceResponse> response = restTemplate
                .getForEntity(baseUrl() + "/" + deviceId, DeviceResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDeviceId()).isEqualTo(deviceId);
    }

    @Test
    void getDevice_nonExistentDevice_returns404() {
        final ResponseEntity<Void> response = restTemplate
                .getForEntity(baseUrl() + "/" + UUID.randomUUID(), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ─── GET ALL / FILTER DEVICES ─────────────────────────────────────────────────

    @Test
    void getAllDevices_returnsAllDevices() {
        createDevice(DEFAULT_NAME, DEFAULT_BRAND, DEFAULT_STATE);
        createDevice(GALAXY_NAME, SAMSUNG_BRAND, State.IN_USE);

        final ResponseEntity<DevicePage> response = restTemplate
                .getForEntity(baseUrl(), DevicePage.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().content).hasSize(2);
        assertThat(response.getBody().totalElements).isEqualTo(2);
    }

    @Test
    void getDevicesByBrand_returnsFilteredDevices() {
        createDevice(DEFAULT_NAME, DEFAULT_BRAND, DEFAULT_STATE);
        createDevice(MACBOOK_NAME, DEFAULT_BRAND, State.IN_USE);
        createDevice(GALAXY_NAME, SAMSUNG_BRAND, DEFAULT_STATE);

        final ResponseEntity<DevicePage> response = restTemplate
                .getForEntity(baseUrl() + "?brand=" + DEFAULT_BRAND, DevicePage.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().content).hasSize(2);
        assertThat(response.getBody().totalElements).isEqualTo(2);
        assertThat(response.getBody().content)
                .allMatch(d -> d.getBrand().equals(DEFAULT_BRAND));
    }

    @Test
    void getDevicesByState_returnsFilteredDevices() {
        createDevice(DEFAULT_NAME, DEFAULT_BRAND, DEFAULT_STATE);
        createDevice(MACBOOK_NAME, DEFAULT_BRAND, State.IN_USE);
        createDevice(GALAXY_NAME, SAMSUNG_BRAND, DEFAULT_STATE);

        final ResponseEntity<DevicePage> response = restTemplate
                .getForEntity(baseUrl() + "?state=" + DEFAULT_STATE, DevicePage.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().content).hasSize(2);
        assertThat(response.getBody().totalElements).isEqualTo(2);
        assertThat(response.getBody().content)
                .allMatch(d -> d.getState() == DEFAULT_STATE);
    }

    @Test
    void getDevices_withBrandAndState_returns400() {
        final ResponseEntity<Void> response = restTemplate
                .getForEntity(baseUrl() + "?brand=" + DEFAULT_BRAND + "&state=" + DEFAULT_STATE, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void getDevices_respectsPageSize() {
        createDevice(DEFAULT_NAME, DEFAULT_BRAND, DEFAULT_STATE);
        createDevice(MACBOOK_NAME, DEFAULT_BRAND, State.IN_USE);
        createDevice(GALAXY_NAME, SAMSUNG_BRAND, DEFAULT_STATE);

        final ResponseEntity<DevicePage> response = restTemplate
                .getForEntity(baseUrl() + "?size=2&page=0", DevicePage.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().content).hasSize(2);
        assertThat(response.getBody().totalElements).isEqualTo(3);
        assertThat(response.getBody().totalPages).isEqualTo(2);
    }

    // ─── FULL UPDATE OF DEVICES ───────────────────────────────────────────────────

    @Test
    void updateDevice_validRequest_returns200() {
        final UUID deviceId = createDevice(DEFAULT_NAME, DEFAULT_BRAND, DEFAULT_STATE);
        final DeviceRequest request = buildRequest(UPDATED_NAME, DEFAULT_BRAND, DEFAULT_STATE);

        final ResponseEntity<DeviceResponse> response = restTemplate.exchange(
                baseUrl() + "/" + deviceId,
                HttpMethod.PUT,
                new HttpEntity<>(request),
                DeviceResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getName()).isEqualTo(UPDATED_NAME);
    }

    @Test
    void updateDevice_deviceInUse_returns409() {
        final UUID deviceId = createDevice(DEFAULT_NAME, DEFAULT_BRAND, State.IN_USE);
        final DeviceRequest request = buildRequest(UPDATED_NAME, DEFAULT_BRAND, State.IN_USE);

        final ResponseEntity<Void> response = restTemplate.exchange(
                baseUrl() + "/" + deviceId,
                HttpMethod.PUT,
                new HttpEntity<>(request),
                Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void updateDevice_nonExistentDevice_returns404() {
        final DeviceRequest request = buildRequest(UPDATED_NAME, DEFAULT_BRAND, DEFAULT_STATE);

        final ResponseEntity<Void> response = restTemplate.exchange(
                baseUrl() + "/" + UUID.randomUUID(),
                HttpMethod.PUT,
                new HttpEntity<>(request),
                Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ─── PARTIAL UPDATE OF DEVICES ────────────────────────────────────────────────

    @Test
    void patchDevice_stateOnly_returns200() {
        final UUID deviceId = createDevice(DEFAULT_NAME, DEFAULT_BRAND, DEFAULT_STATE);
        final DevicePatchRequest request = buildPatchRequestWithState(State.IN_USE);

        final ResponseEntity<DeviceResponse> response = restTemplate.exchange(
                baseUrl() + "/" + deviceId,
                HttpMethod.PATCH,
                new HttpEntity<>(request),
                DeviceResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getState()).isEqualTo(State.IN_USE);
    }

    @Test
    void patchDevice_nameChangeOnInUseDevice_returns409() {
        final UUID deviceId = createDevice(DEFAULT_NAME, DEFAULT_BRAND, State.IN_USE);
        final DevicePatchRequest request = buildPatchRequestWithName(UPDATED_NAME);

        final ResponseEntity<Void> response = restTemplate.exchange(
                baseUrl() + "/" + deviceId,
                HttpMethod.PATCH,
                new HttpEntity<>(request),
                Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void patchDevice_stateChangeOnInUseDevice_returns200() {
        final UUID deviceId = createDevice(DEFAULT_NAME, DEFAULT_BRAND, State.IN_USE);
        final DevicePatchRequest request = buildPatchRequestWithState(State.INACTIVE);

        final ResponseEntity<DeviceResponse> response = restTemplate.exchange(
                baseUrl() + "/" + deviceId,
                HttpMethod.PATCH,
                new HttpEntity<>(request),
                DeviceResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getState()).isEqualTo(State.INACTIVE);
    }

    // ─── DELETE DEVICE ────────────────────────────────────────────────────────────

    @Test
    void deleteDevice_availableDevice_returns204() {
        final UUID deviceId = createDevice(DEFAULT_NAME, DEFAULT_BRAND, DEFAULT_STATE);

        final ResponseEntity<Void> response = restTemplate.exchange(
                baseUrl() + "/" + deviceId,
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(deviceRepository.findByDeviceId(deviceId)).isEmpty();
    }

    @Test
    void deleteDevice_inUseDevice_returns409() {
        final UUID deviceId = createDevice(DEFAULT_NAME, DEFAULT_BRAND, State.IN_USE);

        final ResponseEntity<Void> response = restTemplate.exchange(
                baseUrl() + "/" + deviceId,
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void deleteDevice_nonExistentDevice_returns404() {
        final ResponseEntity<Void> response = restTemplate.exchange(
                baseUrl() + "/" + UUID.randomUUID(),
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ─── HELPER METHODS ───────────────────────────────────────────────────────────

    private UUID createDevice(final String name, final String brand, final State state) {
        final ResponseEntity<DeviceResponse> response = restTemplate
                .postForEntity(baseUrl(), buildRequest(name, brand, state), DeviceResponse.class);
        return response.getBody().getDeviceId();
    }
}
