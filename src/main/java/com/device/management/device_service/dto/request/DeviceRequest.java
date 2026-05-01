package com.device.management.device_service.dto.request;

import com.device.management.device_service.domain.State;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class DeviceRequest extends DeviceBaseRequest {

    @NotBlank
    @Override
    public String getName() { return super.getName(); }

    @NotBlank
    @Override
    public String getBrand() { return super.getBrand(); }

    @NotNull
    @Override
    public State getState() { return super.getState(); }

}
