package com.fierceadventurer.smartportfoliobackend.payment.controller;

import com.fierceadventurer.smartportfoliobackend.payment.dto.SponsorResponse;
import com.fierceadventurer.smartportfoliobackend.payment.service.SponsorQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sponsors") // Public route! No admin secret needed.
@RequiredArgsConstructor
public class SponsorController {

    private final SponsorQueryService sponsorQueryService;

    @GetMapping
     public ResponseEntity<List<SponsorResponse>> getSponsors() {
        return ResponseEntity.ok(sponsorQueryService.getRecentSponsors());
    }
}
