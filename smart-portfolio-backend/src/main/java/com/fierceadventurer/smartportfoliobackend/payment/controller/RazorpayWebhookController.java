package com.fierceadventurer.smartportfoliobackend.payment.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fierceadventurer.smartportfoliobackend.payment.service.PaymentModuleService;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/webhooks/razorpay")
@RequiredArgsConstructor
public class RazorpayWebhookController {
    private final PaymentModuleService paymentModuleService;
    private final ObjectMapper objectMapper;

    @Value("${razorpay.webhook-secret}")
    private String webhookSecret;

    @PostMapping
    public ResponseEntity<String> handleRazorpayWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature
    ){
        try{
            //verify signature
            boolean isSignatureValid = Utils.verifyWebhookSignature(payload, signature, webhookSecret);

            if(!isSignatureValid){
                log.warn("Invalid Razorpay webhook signature detected!");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid Signature");
            }

            // parse event
            JsonNode rootNode = objectMapper.readTree(payload);
            String eventType = rootNode.path("event").asText();

            // razorpay includes unique id for every webhook event it fires
            String razorpayEventId = rootNode.path("id").asText();

            log.info("Verified Razorpay webhook. Event Type: {}", eventType);

            // Handle captured payment
            if("payment.captured".equals(eventType)){
                JsonNode paymentEntity = rootNode.path("payload").path("payment").path("entity");
                String paymentId = paymentEntity.path("id").asText();
                String email = paymentEntity.path("email").asText();
                String currency = paymentEntity.path("currency").asText();

                // razorpay amount in paise conversion in inr bhu dividing by 100
                double amount = paymentEntity.path("amount").asDouble()/100.0;

                // extract the user name from custom "notes" object
                JsonNode notes = paymentEntity.path("notes");
                String name = notes.path("sponsor_name").asText("Anonymous Sponsor");

                //execute outbox transaction (db acting as queue for further microservice)
                paymentModuleService.processSuccessfulSponsorship(razorpayEventId, paymentId, name , email , amount , currency);

                return ResponseEntity.ok("Sponsorship processed successfully.");
            }
            return ResponseEntity.ok("Event received but ignored.");

        }catch (org.springframework.dao.DuplicateKeyException e) {
            // This catches the unique constraint violation on your event_id column!
            log.info("Duplicate webhook event received from Razorpay. Safely ignored.");
            return ResponseEntity.ok("Duplicate event ignored.");

        } catch (Exception e) {
            log.error("Failed to process Razorpay webhook payload", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Webhook processing failed");
        }

    }
}
