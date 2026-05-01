package com.device.management.device_service.repository;

import com.device.management.device_service.config.DomainConfig;
import com.device.management.device_service.domain.DeviceEntity;
import com.device.management.device_service.domain.State;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.device.management.device_service.TestFactory.*;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@Import(DomainConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class DeviceRepositoryTest {

    private static final String MACBOOK_NAME = "MacBook Pro";
    private static final String SAMSUNG_BRAND = "Samsung";
    private static final String GALAXY_NAME = "Galaxy S24";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private DeviceRepository deviceRepository;

    @BeforeEach
    void cleanUp() {
        deviceRepository.deleteAll();
    }

    @Test
    void findByDeviceId_existingDevice_returnsDevice() {
        final DeviceEntity saved = saveDevice(DEFAULT_NAME, DEFAULT_BRAND, DEFAULT_STATE);

        final Optional<DeviceEntity> result = deviceRepository.findByDeviceId(saved.getDeviceId());

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo(DEFAULT_NAME);
        assertThat(result.get().getBrand()).isEqualTo(DEFAULT_BRAND);
        assertThat(result.get().getState()).isEqualTo(DEFAULT_STATE);
    }

    @Test
    void findByDeviceId_nonExistentDevice_returnsEmpty() {
        final Optional<DeviceEntity> result = deviceRepository.findByDeviceId(UUID.randomUUID());

        assertThat(result).isEmpty();
    }

    @Test
    void findByBrand_returnsOnlyMatchingBrand() {
        saveDevice(DEFAULT_NAME, DEFAULT_BRAND, DEFAULT_STATE);
        saveDevice(MACBOOK_NAME, DEFAULT_BRAND, State.IN_USE);
        saveDevice(GALAXY_NAME, SAMSUNG_BRAND, DEFAULT_STATE);

        final Page<DeviceEntity> result = deviceRepository.findByBrand(
                DEFAULT_BRAND, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).allMatch(d -> d.getBrand().equals(DEFAULT_BRAND));
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void findByState_returnsOnlyMatchingState() {
        saveDevice(DEFAULT_NAME, DEFAULT_BRAND, DEFAULT_STATE);
        saveDevice(MACBOOK_NAME, DEFAULT_BRAND, State.IN_USE);
        saveDevice(GALAXY_NAME, SAMSUNG_BRAND, DEFAULT_STATE);

        final Page<DeviceEntity> result = deviceRepository.findByState(
                DEFAULT_STATE, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).allMatch(d -> d.getState() == DEFAULT_STATE);
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    // ─── HELPERS ───────────────────────────────────────────────────────────

    private DeviceEntity saveDevice(final String name, final String brand, final State state) {
        return deviceRepository.save(buildEntity(name, brand, state));
    }
}
