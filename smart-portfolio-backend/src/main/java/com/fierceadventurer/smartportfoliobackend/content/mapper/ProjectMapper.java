package com.fierceadventurer.smartportfoliobackend.content.mapper;

import com.fierceadventurer.smartportfoliobackend.content.dto.ProjectResponse;
import com.fierceadventurer.smartportfoliobackend.content.entity.Project;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ProjectMapper {
    ProjectResponse toResponse(Project project);
}
