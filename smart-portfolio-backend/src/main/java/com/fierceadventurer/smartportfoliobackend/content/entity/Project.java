package com.fierceadventurer.smartportfoliobackend.content.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "projects")
@Data
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false , nullable = false)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false , columnDefinition = "TEXT")
    private String description;

    @Column(name = "tech_stack")
    private String techStack;

    @Column(name = "github_url")
    private String githubUrl;

    @Column(name = "liveUrl")
    private String liveUrl;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
