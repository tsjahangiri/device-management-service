package com.device.management.device_service.service;

import com.device.management.device_service.domain.DeviceEntity;
import com.device.management.device_service.domain.State;
import com.device.management.device_service.dto.request.DevicePatchRequest;
import com.device.management.device_service.dto.request.DeviceRequest;
import com.device.management.device_service.dto.response.DeviceResponse;
import com.device.management.device_service.exception.DataPersistenceException;
import com.device.management.device_service.exception.DeviceNotDeletableException;
import com.device.management.device_service.exception.DeviceNotFoundException;
import com.device.management.device_service.exception.DeviceNotUpdatableException;
import com.device.management.device_service.exception.InvalidFilterException;
import com.device.management.device_service.repository.DeviceRepository;
import com.device.management.device_service.transform.DeviceMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final DeviceMapper deviceMapper;

    public DeviceService(final DeviceRepository deviceRepository,
                         final DeviceMapper deviceMapper) {
        this.deviceRepository = deviceRepository;
        this.deviceMapper = deviceMapper;
    }

    @Transactional
    public DeviceResponse createDevice(final DeviceRequest deviceRequest) {
        final DeviceEntity deviceEntity = this.deviceMapper.toDeviceEntity(deviceRequest);
        final DeviceEntity savedEntity = persistSave(deviceEntity);
        return this.deviceMapper.toDeviceResponse(savedEntity);
    }

    @Transactional
    public DeviceResponse updateDevice(final UUID deviceId, final DeviceRequest deviceRequest) {
        final DeviceEntity deviceEntity = findByDeviceIdOrThrow(deviceId);

        if (deviceEntity.getState() == State.IN_USE) {
            throw new DeviceNotUpdatableException(deviceId);
        }

        this.deviceMapper.updateDeviceEntity(deviceRequest, deviceEntity);
        final DeviceEntity savedEntity = persistSave(deviceEntity);
        return this.deviceMapper.toDeviceResponse(savedEntity);
    }

    @Transactional
    public DeviceResponse patchDevice(final UUID deviceId, final DevicePatchRequest deviceRequest) {
        final DeviceEntity deviceEntity = findByDeviceIdOrThrow(deviceId);

        if (deviceEntity.getState() == State.IN_USE) {
            if (deviceRequest.getName() != null || deviceRequest.getBrand() != null) {
                throw new DeviceNotUpdatableException(deviceId);
            }
        }

        if (deviceRequest.getName() != null) {
            deviceEntity.setName(deviceRequest.getName());
        }
        if (deviceRequest.getBrand() != null) {
            deviceEntity.setBrand(deviceRequest.getBrand());
        }
        if (deviceRequest.getState() != null) {
            deviceEntity.setState(deviceRequest.getState());
        }

        final DeviceEntity savedEntity = persistSave(deviceEntity);
        return this.deviceMapper.toDeviceResponse(savedEntity);
    }

    @Transactional(readOnly = true)
    public DeviceResponse getDevice(final UUID deviceId) {
        return this.deviceRepository.findByDeviceId(deviceId)
                .map(this.deviceMapper::toDeviceResponse)
                .orElseThrow(() -> new DeviceNotFoundException(deviceId));
    }

    @Transactional(readOnly = true)
    public Page<DeviceResponse> getDevices(final String brand, final State state,
                                           final Pageable pageable) {
        if (brand != null && state != null) {
            throw new InvalidFilterException(
                    "Only one filter parameter is allowed at a time: 'brand' or 'state'");
        }
        if (brand != null) {
            return getDevicesByBrand(brand, pageable);
        }
        if (state != null) {
            return getDevicesByState(state, pageable);
        }
        return getAllDevices(pageable);
    }

    @Transactional
    public void deleteDevice(final UUID deviceId) {
        final DeviceEntity deviceEntity = findByDeviceIdOrThrow(deviceId);

        if (deviceEntity.getState() == State.IN_USE) {
            throw new DeviceNotDeletableException(deviceId);
        }

        persistDelete(deviceEntity);
    }

    // ─── Private helpers ───────────────────────────────────────────────────

    private DeviceEntity findByDeviceIdOrThrow(final UUID deviceId) {
        return this.deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new DeviceNotFoundException(deviceId));
    }

    private Page<DeviceResponse> getAllDevices(final Pageable pageable) {
        return this.deviceRepository.findAll(pageable)
                .map(this.deviceMapper::toDeviceResponse);
    }

    private Page<DeviceResponse> getDevicesByBrand(final String brand, final Pageable pageable) {
        return this.deviceRepository.findByBrand(brand, pageable)
                .map(this.deviceMapper::toDeviceResponse);
    }

    private Page<DeviceResponse> getDevicesByState(final State state, final Pageable pageable) {
        return this.deviceRepository.findByState(state, pageable)
                .map(this.deviceMapper::toDeviceResponse);
    }

    private DeviceEntity persistSave(final DeviceEntity deviceEntity) {
        try {
            return this.deviceRepository.save(deviceEntity);
        } catch (DataIntegrityViolationException ex) {
            throw new DataPersistenceException(
                    "Device could not be saved due to a data integrity violation");
        } catch (DataAccessException ex) {
            throw new DataPersistenceException(
                    "Device could not be saved due to a database error");
        }
    }

    private void persistDelete(final DeviceEntity deviceEntity) {
        try {
            this.deviceRepository.delete(deviceEntity);
        } catch (DataAccessException ex) {
            throw new DataPersistenceException(
                    "Device could not be deleted due to a database error");
        }
    }
}
