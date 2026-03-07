package com.fierceadventurer.smartportfoliobackend.content.service;

import com.fierceadventurer.smartportfoliobackend.content.dto.ProjectRequest;
import com.fierceadventurer.smartportfoliobackend.content.repository.ProjectRepository;
import com.fierceadventurer.smartportfoliobackend.content.dto.ProjectResponse;
import com.fierceadventurer.smartportfoliobackend.content.mapper.ProjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {
    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;


    @Override
    @Cacheable(value = "portfolio_projects")
    public List<ProjectResponse> getAllProjects() {
        log.info("Fetching all projects from NeonDB (cache miss");

        return projectRepository.findAll().stream()
                .map(projectMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @CacheEvict(value = "portfolio_projects", allEntries = true)
    public ProjectResponse createProject(ProjectRequest request) {
        log.info("Creating a new project: {}", request.title());

        var entity = projectMapper.toEntity(request);
        var savedEntity = projectRepository.save(entity);

        return projectMapper.toResponse(savedEntity);
    }
}
