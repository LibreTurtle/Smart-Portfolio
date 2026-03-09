package com.fierceadventurer.smartportfoliobackend.payment.event;

// payload The JSON string containing sponsorName, amount, currency, and email.
public record SponsorPaidEvent(
        String payload
) {
}
