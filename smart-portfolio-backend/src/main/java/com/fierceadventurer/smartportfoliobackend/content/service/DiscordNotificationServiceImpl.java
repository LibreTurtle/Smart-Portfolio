package com.fierceadventurer.smartportfoliobackend.content.service;

import com.fierceadventurer.smartportfoliobackend.content.dto.ContactMessageRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Service
public class DiscordNotificationServiceImpl implements DiscordNotificationService{
    private final String webhookUrl;
    private final RestClient restClient;

    public DiscordNotificationServiceImpl(
            @Value("${portfolio.notification.discord.webhook-url}") String webhookUrl){
                this.webhookUrl = webhookUrl;
                this.restClient = RestClient.create();
    }

    @Async
    @Override
    public void sendContactNotification(ContactMessageRequest request) {
        // Format a nice markdown message for Discord
        String discordMessage = String.format(
                "**New Portfolio Contact Message!**\n**Name:** %s\n**Email:** %s\n**Message:**\n```text\n%s\n```",
                request.senderName(),
                request.senderEmail(),
                request.messageBody()
        );

        this.sendNotification(discordMessage);


    }

    @Override
    public void sendNotification(String message) {
        if(webhookUrl == null || webhookUrl.isBlank()){
            log.warn("Discord webhook URL is not configured. Skipping notification.");
            return;
        }
        try {


            // Discord expects Json object with "content " field
            Map<String , String> payload = Map.of("content", message);

            restClient.post()
                    .uri(webhookUrl)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Successfully send async Discord notification.");
        }
        catch (Exception e){
            log.error("Failed to send Discord Notification", e);
        }
    }


}
