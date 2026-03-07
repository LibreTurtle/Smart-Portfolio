package com.fierceadventurer.smartportfoliobackend.content.mapper;

import com.fierceadventurer.smartportfoliobackend.content.dto.ContactMessageRequest;
import com.fierceadventurer.smartportfoliobackend.content.dto.ContactMessageResponse;
import com.fierceadventurer.smartportfoliobackend.content.entity.ContactMessage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ContactMessageMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isRead", ignore = true)
    @Mapping(target = "submittedAt", ignore = true)
    ContactMessage toEntity(ContactMessageRequest request);

    ContactMessageResponse toResponse(ContactMessage entity);
}
