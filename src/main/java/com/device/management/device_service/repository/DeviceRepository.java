package com.device.management.device_service.repository;

import com.device.management.device_service.domain.DeviceEntity;
import com.device.management.device_service.domain.State;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface DeviceRepository extends JpaRepository<DeviceEntity, Long> {

    Optional<DeviceEntity> findByDeviceId(UUID deviceId);

    Page<DeviceEntity> findByBrand(String brand, Pageable pageable);

    Page<DeviceEntity> findByState(State state, Pageable pageable);
}
