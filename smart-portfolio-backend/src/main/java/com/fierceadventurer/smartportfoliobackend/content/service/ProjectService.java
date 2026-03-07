package com.fierceadventurer.smartportfoliobackend.content.service;

import com.fierceadventurer.smartportfoliobackend.content.dto.ProjectRequest;
import com.fierceadventurer.smartportfoliobackend.content.dto.ProjectResponse;

import java.util.List;

public interface ProjectService {
    List<ProjectResponse> getAllProjects();

    ProjectResponse createProject(ProjectRequest request);
}
