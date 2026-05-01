package com.device.management.device_service.transform;

import com.device.management.device_service.domain.DeviceEntity;
import com.device.management.device_service.domain.State;
import com.device.management.device_service.dto.request.DeviceRequest;
import com.device.management.device_service.dto.response.DeviceResponse;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.UUID;

import static com.device.management.device_service.TestFactory.*;
import static org.assertj.core.api.Assertions.assertThat;

public class DeviceMapperTest {

    private static final String UPDATED_NAME = "New Name";
    private static final String UPDATED_BRAND = "New Brand";

    private final DeviceMapper deviceMapper = Mappers.getMapper(DeviceMapper.class);

    @Test
    void toDeviceEntity_mapsCorrectly() {
        final DeviceRequest request = buildValidRequest();

        final DeviceEntity entity = deviceMapper.toDeviceEntity(request);

        assertThat(entity.getName()).isEqualTo(DEFAULT_NAME);
        assertThat(entity.getBrand()).isEqualTo(DEFAULT_BRAND);
        assertThat(entity.getState()).isEqualTo(DEFAULT_STATE);
        assertThat(entity.getId()).isNull();
        assertThat(entity.getDeviceId()).isNull();
        assertThat(entity.getDateCreated()).isNull();
        assertThat(entity.getLastUpdated()).isNull();
    }

    @Test
    void toDeviceResponse_mapsCorrectly() {
        final UUID deviceId = UUID.randomUUID();
        final DeviceEntity entity = buildEntity(deviceId, DEFAULT_STATE);

        final DeviceResponse response = deviceMapper.toDeviceResponse(entity);

        assertThat(response.getDeviceId()).isEqualTo(deviceId);
        assertThat(response.getName()).isEqualTo(DEFAULT_NAME);
        assertThat(response.getBrand()).isEqualTo(DEFAULT_BRAND);
        assertThat(response.getState()).isEqualTo(DEFAULT_STATE);
    }

    @Test
    void updateDeviceEntity_updatesOnlyMappedFields() {
        final DeviceEntity entity = buildEntity(DEFAULT_STATE);
        final DeviceRequest request = buildRequest(UPDATED_NAME, UPDATED_BRAND, State.IN_USE);

        deviceMapper.updateDeviceEntity(request, entity);

        assertThat(entity.getName()).isEqualTo(UPDATED_NAME);
        assertThat(entity.getBrand()).isEqualTo(UPDATED_BRAND);
        assertThat(entity.getState()).isEqualTo(State.IN_USE);
    }
}

