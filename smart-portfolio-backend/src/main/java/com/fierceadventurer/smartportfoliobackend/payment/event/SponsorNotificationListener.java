package com.fierceadventurer.smartportfoliobackend.payment.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fierceadventurer.smartportfoliobackend.content.service.DiscordNotificationServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SponsorNotificationListener {

    private final ObjectMapper objectMapper;
    private final DiscordNotificationServiceImpl discordService;

    @Async
    @EventListener
    public void onSponsorPaid(SponsorPaidEvent event){
        log.info("======================================================");
        log.info("EVENT BUS INTERCEPT: New Sponsorship Processed!");
        log.info("======================================================");

        try{
            // paarse json payload
            JsonNode payloadNode = objectMapper.readTree(event.payload());
            String name = payloadNode.path("sponsorName").asText();
            double amount = payloadNode.path("amount").asDouble();
            String currency = payloadNode.path("currency").asText();
            String email = payloadNode.path("email").asText();

            String discordMessage = String.format(
                "🎉 **NEW SPONSOR ALERT!** 🎉\n" +
                "> **Name:** %s\n" +
                "> **Amount:** %.2f %s\n" +
                "> **Email:** %s\n" +
                "Awesome job, the Outbox pipeline worked perfectly!",
                name, amount, currency, email
            );

            //send it to phone
            discordService.sendNotification(discordMessage);
            log.info("Successfully pushed Sponsor alert to Discord.");

        }
        catch (Exception e) {
            log.error("Failed to parse Sponsor payload or send Discord notification", e);
        }
    }
}
