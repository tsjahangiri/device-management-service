package com.device.management.device_service.transform;

import com.device.management.device_service.domain.DeviceEntity;
import com.device.management.device_service.dto.request.DeviceRequest;
import com.device.management.device_service.dto.response.DeviceResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface DeviceMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deviceId", ignore = true)
    @Mapping(target = "dateCreated", ignore = true)
    @Mapping(target = "lastUpdated", ignore = true)
    DeviceEntity toDeviceEntity(final DeviceRequest deviceRequest);

    @Mapping(target = "deviceId", source = "deviceId")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "brand", source = "brand")
    @Mapping(target = "state", source = "state")
    DeviceResponse toDeviceResponse(DeviceEntity deviceEntity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deviceId", ignore = true)
    @Mapping(target = "dateCreated", ignore = true)
    @Mapping(target = "lastUpdated", ignore = true)
    void updateDeviceEntity(DeviceRequest request, @MappingTarget DeviceEntity entity);

}
