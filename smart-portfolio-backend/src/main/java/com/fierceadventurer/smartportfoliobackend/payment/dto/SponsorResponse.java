package com.fierceadventurer.smartportfoliobackend.payment.dto;

public record SponsorResponse(
    String sponsorName,
    double amount,
    String currency
) { }
