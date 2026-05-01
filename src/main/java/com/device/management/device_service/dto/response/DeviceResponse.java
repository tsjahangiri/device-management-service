package com.device.management.device_service.dto.response;

import com.device.management.device_service.domain.State;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceResponse {

    private UUID deviceId;
    private String name;
    private String brand;
    private State state;
    private OffsetDateTime dateCreated;

}
