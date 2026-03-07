package com.fierceadventurer.smartportfoliobackend.content.controller;

import com.fierceadventurer.smartportfoliobackend.content.dto.ProjectRequest;
import com.fierceadventurer.smartportfoliobackend.content.dto.ProjectResponse;
import com.fierceadventurer.smartportfoliobackend.content.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {
    private  final ProjectService projectService;

    @GetMapping
    public ResponseEntity <List<ProjectResponse>> getAllProjects(){
        return ResponseEntity.ok(projectService.getAllProjects());
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(@Valid @RequestBody ProjectRequest request){
        ProjectResponse response = projectService.createProject(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
