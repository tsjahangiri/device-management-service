package com.device.management.device_service.service;

import com.device.management.device_service.domain.DeviceEntity;
import com.device.management.device_service.domain.State;
import com.device.management.device_service.dto.request.DevicePatchRequest;
import com.device.management.device_service.dto.request.DeviceRequest;
import com.device.management.device_service.dto.response.DeviceResponse;
import com.device.management.device_service.exception.DeviceNotDeletableException;
import com.device.management.device_service.exception.DeviceNotFoundException;
import com.device.management.device_service.exception.DeviceNotUpdatableException;
import com.device.management.device_service.exception.InvalidFilterException;
import com.device.management.device_service.repository.DeviceRepository;
import com.device.management.device_service.transform.DeviceMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.device.management.device_service.TestFactory.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DeviceServiceTest {

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private DeviceMapper deviceMapper;

    @InjectMocks
    private DeviceService deviceService;

    // ─── CREATE ────────────────────────────────────────────────────────────

    @Test
    void createDevice_validRequest_savesAndReturnsResponse() {
        final DeviceRequest request = buildValidRequest();
        final DeviceEntity entity = buildEntity(State.AVAILABLE);
        final UUID deviceId = UUID.randomUUID();
        final DeviceResponse expectedResponse = buildValidResponse(deviceId);

        when(deviceMapper.toDeviceEntity(request)).thenReturn(entity);
        when(deviceRepository.save(entity)).thenReturn(entity);
        when(deviceMapper.toDeviceResponse(entity)).thenReturn(expectedResponse);

        final DeviceResponse result = deviceService.createDevice(request);

        verify(deviceRepository).save(entity);
        assertThat(result).isEqualTo(expectedResponse);
        assertThat(result.getDeviceId()).isEqualTo(deviceId);
    }

    // ─── GET SINGLE ────────────────────────────────────────────────────────

    @Test
    void getDevice_existingDevice_returnsResponse() {
        final UUID deviceId = UUID.randomUUID();
        final DeviceEntity entity = buildEntity(deviceId, State.AVAILABLE);
        final DeviceResponse expectedResponse = buildValidResponse(deviceId);

        when(deviceRepository.findByDeviceId(deviceId)).thenReturn(Optional.of(entity));
        when(deviceMapper.toDeviceResponse(entity)).thenReturn(expectedResponse);

        final DeviceResponse result = deviceService.getDevice(deviceId);

        assertThat(result).isEqualTo(expectedResponse);
        assertThat(result.getDeviceId()).isEqualTo(deviceId);
    }

    @Test
    void getDevice_nonExistentDevice_throwsDeviceNotFoundException() {
        final UUID deviceId = UUID.randomUUID();

        when(deviceRepository.findByDeviceId(deviceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deviceService.getDevice(deviceId))
                .isInstanceOf(DeviceNotFoundException.class)
                .hasMessageContaining(deviceId.toString());
    }

    // ─── GET ALL / FILTER ──────────────────────────────────────────────────

    @Test
    void getDevices_noBrandNoState_returnsAllDevices() {
        final UUID deviceId1 = UUID.randomUUID();
        final UUID deviceId2 = UUID.randomUUID();

        final DeviceEntity entity1 = buildEntity(deviceId1, State.AVAILABLE);
        final DeviceEntity entity2 = buildEntity(deviceId2, State.IN_USE);

        final DeviceResponse response1 = buildValidResponse(deviceId1);
        final DeviceResponse response2 = buildValidResponse(deviceId2);

        final Pageable pageable = PageRequest.of(0, 20);
        final Page<DeviceEntity> entityPage = new PageImpl<>(List.of(entity1, entity2), pageable, 2);

        when(deviceRepository.findAll(pageable)).thenReturn(entityPage);
        when(deviceMapper.toDeviceResponse(entity1)).thenReturn(response1);
        when(deviceMapper.toDeviceResponse(entity2)).thenReturn(response2);

        final Page<DeviceResponse> result = deviceService.getDevices(null, null, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).containsExactly(response1, response2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        verify(deviceRepository).findAll(pageable);
        verify(deviceRepository, never()).findByBrand(any(), any());
        verify(deviceRepository, never()).findByState(any(), any());
    }

    @Test
    void getDevices_withBrand_returnsFilteredByBrand() {
        final UUID deviceId = UUID.randomUUID();
        final DeviceEntity entity = buildEntity(deviceId, State.AVAILABLE);
        final DeviceResponse response = buildValidResponse(deviceId);

        final Pageable pageable = PageRequest.of(0, 20);
        final Page<DeviceEntity> entityPage = new PageImpl<>(List.of(entity), pageable, 1);

        when(deviceRepository.findByBrand(DEFAULT_BRAND, pageable)).thenReturn(entityPage);
        when(deviceMapper.toDeviceResponse(entity)).thenReturn(response);

        final Page<DeviceResponse> result = deviceService.getDevices(DEFAULT_BRAND, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getDeviceId()).isEqualTo(deviceId);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(deviceRepository).findByBrand(DEFAULT_BRAND, pageable);
        verify(deviceRepository, never()).findAll(any(Pageable.class));
        verify(deviceRepository, never()).findByState(any(), any());
    }

    @Test
    void getDevices_withState_returnsFilteredByState() {
        final UUID deviceId = UUID.randomUUID();
        final DeviceEntity entity = buildEntity(deviceId, State.AVAILABLE);
        final DeviceResponse response = buildValidResponse(deviceId);

        final Pageable pageable = PageRequest.of(0, 20);
        final Page<DeviceEntity> entityPage = new PageImpl<>(List.of(entity), pageable, 1);

        when(deviceRepository.findByState(State.AVAILABLE, pageable)).thenReturn(entityPage);
        when(deviceMapper.toDeviceResponse(entity)).thenReturn(response);

        final Page<DeviceResponse> result = deviceService.getDevices(null, State.AVAILABLE, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getDeviceId()).isEqualTo(deviceId);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(deviceRepository).findByState(State.AVAILABLE, pageable);
        verify(deviceRepository, never()).findAll(any(Pageable.class));
        verify(deviceRepository, never()).findByBrand(any(), any());
    }

    @Test
    void getDevices_withBrandAndState_throwsInvalidFilterException() {
        final Pageable pageable = PageRequest.of(0, 20);

        assertThatThrownBy(() -> deviceService.getDevices(DEFAULT_BRAND, State.AVAILABLE, pageable))
                .isInstanceOf(InvalidFilterException.class)
                .hasMessageContaining("Only one filter parameter is allowed");

        verify(deviceRepository, never()).findAll(any(Pageable.class));
        verify(deviceRepository, never()).findByBrand(any(), any());
        verify(deviceRepository, never()).findByState(any(), any());
    }

    // ─── FULL UPDATE ───────────────────────────────────────────────────────

    @Test
    void updateDevice_availableDevice_updatesSuccessfully() {
        final UUID deviceId = UUID.randomUUID();
        final DeviceRequest request = buildValidRequest();
        final DeviceEntity entity = buildEntity(deviceId, State.AVAILABLE);
        final DeviceResponse expectedResponse = buildValidResponse(deviceId);

        when(deviceRepository.findByDeviceId(deviceId)).thenReturn(Optional.of(entity));
        when(deviceRepository.save(entity)).thenReturn(entity);
        when(deviceMapper.toDeviceResponse(entity)).thenReturn(expectedResponse);

        final DeviceResponse result = deviceService.updateDevice(deviceId, request);

        verify(deviceMapper).updateDeviceEntity(request, entity);
        verify(deviceRepository).save(entity);
        assertThat(result).isEqualTo(expectedResponse);
    }

    @Test
    void updateDevice_deviceInUse_throwsDeviceNotUpdatableException() {
        final UUID deviceId = UUID.randomUUID();
        final DeviceRequest request = buildValidRequest();
        final DeviceEntity entity = buildEntity(deviceId, State.IN_USE);

        when(deviceRepository.findByDeviceId(deviceId)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> deviceService.updateDevice(deviceId, request))
                .isInstanceOf(DeviceNotUpdatableException.class)
                .hasMessageContaining(deviceId.toString());

        verify(deviceRepository, never()).save(any());
    }

    @Test
    void updateDevice_nonExistentDevice_throwsDeviceNotFoundException() {
        final UUID deviceId = UUID.randomUUID();

        when(deviceRepository.findByDeviceId(deviceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deviceService.updateDevice(deviceId, buildValidRequest()))
                .isInstanceOf(DeviceNotFoundException.class)
                .hasMessageContaining(deviceId.toString());
    }

    // ─── PARTIAL UPDATE ────────────────────────────────────────────────────

    @Test
    void patchDevice_stateOnly_updatesSuccessfully() {
        final UUID deviceId = UUID.randomUUID();
        final DevicePatchRequest request = buildPatchRequestWithState(State.IN_USE);
        final DeviceEntity entity = buildEntity(deviceId, State.AVAILABLE);
        final DeviceResponse expectedResponse = buildValidResponse(deviceId);

        when(deviceRepository.findByDeviceId(deviceId)).thenReturn(Optional.of(entity));
        when(deviceRepository.save(entity)).thenReturn(entity);
        when(deviceMapper.toDeviceResponse(entity)).thenReturn(expectedResponse);

        deviceService.patchDevice(deviceId, request);

        verify(deviceRepository).save(entity);
        assertThat(entity.getState()).isEqualTo(State.IN_USE);
    }

    @Test
    void patchDevice_nameChangeOnInUseDevice_throwsDeviceNotUpdatableException() {
        final UUID deviceId = UUID.randomUUID();
        final DevicePatchRequest request = buildPatchRequestWithName("New Name");
        final DeviceEntity entity = buildEntity(deviceId, State.IN_USE);

        when(deviceRepository.findByDeviceId(deviceId)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> deviceService.patchDevice(deviceId, request))
                .isInstanceOf(DeviceNotUpdatableException.class)
                .hasMessageContaining(deviceId.toString());

        verify(deviceRepository, never()).save(any());
    }

    @Test
    void patchDevice_brandChangeOnInUseDevice_throwsDeviceNotUpdatableException() {
        final UUID deviceId = UUID.randomUUID();
        final DevicePatchRequest request = buildPatchRequestWithBrand("Samsung");
        final DeviceEntity entity = buildEntity(deviceId, State.IN_USE);

        when(deviceRepository.findByDeviceId(deviceId)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> deviceService.patchDevice(deviceId, request))
                .isInstanceOf(DeviceNotUpdatableException.class)
                .hasMessageContaining(deviceId.toString());

        verify(deviceRepository, never()).save(any());
    }

    @Test
    void patchDevice_stateChangeOnInUseDevice_updatesSuccessfully() {
        final UUID deviceId = UUID.randomUUID();
        final DevicePatchRequest request = buildPatchRequestWithState(State.INACTIVE);
        final DeviceEntity entity = buildEntity(deviceId, State.IN_USE);
        final DeviceResponse expectedResponse = buildValidResponse(deviceId);

        when(deviceRepository.findByDeviceId(deviceId)).thenReturn(Optional.of(entity));
        when(deviceRepository.save(entity)).thenReturn(entity);
        when(deviceMapper.toDeviceResponse(entity)).thenReturn(expectedResponse);

        deviceService.patchDevice(deviceId, request);

        verify(deviceRepository).save(entity);
        assertThat(entity.getState()).isEqualTo(State.INACTIVE);
    }

    @Test
    void patchDevice_nonExistentDevice_throwsDeviceNotFoundException() {
        final UUID deviceId = UUID.randomUUID();
        final DevicePatchRequest request = buildPatchRequestWithState(State.AVAILABLE);

        when(deviceRepository.findByDeviceId(deviceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deviceService.patchDevice(deviceId, request))
                .isInstanceOf(DeviceNotFoundException.class)
                .hasMessageContaining(deviceId.toString());
    }

    // ─── DELETE ────────────────────────────────────────────────────────────

    @Test
    void deleteDevice_availableDevice_deletesSuccessfully() {
        final UUID deviceId = UUID.randomUUID();
        final DeviceEntity entity = buildEntity(deviceId, State.AVAILABLE);

        when(deviceRepository.findByDeviceId(deviceId)).thenReturn(Optional.of(entity));

        deviceService.deleteDevice(deviceId);

        verify(deviceRepository).delete(entity);
    }

    @Test
    void deleteDevice_inUseDevice_throwsDeviceNotDeletableException() {
        final UUID deviceId = UUID.randomUUID();
        final DeviceEntity entity = buildEntity(deviceId, State.IN_USE);

        when(deviceRepository.findByDeviceId(deviceId)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> deviceService.deleteDevice(deviceId))
                .isInstanceOf(DeviceNotDeletableException.class)
                .hasMessageContaining(deviceId.toString());

        verify(deviceRepository, never()).delete(any());
    }

    @Test
    void deleteDevice_nonExistentDevice_throwsDeviceNotFoundException() {
        final UUID deviceId = UUID.randomUUID();

        when(deviceRepository.findByDeviceId(deviceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deviceService.deleteDevice(deviceId))
                .isInstanceOf(DeviceNotFoundException.class)
                .hasMessageContaining(deviceId.toString());
    }
}
