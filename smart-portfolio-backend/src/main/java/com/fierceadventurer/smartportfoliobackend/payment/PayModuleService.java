package com.fierceadventurer.smartportfoliobackend.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayModuleService {

    private final JdbcClient jdbcClient;

    @Transactional
    public void processSuccessfulSponsorship(String paymentId , String name , String email ,
                                             double amount, String currency , String message){

        UUID sponsorId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        log.info("Processing verified Razorpay payment. Initiating Outbox transaction for Sponsor: {}", email);

        // save to sponsor table
        String insertSponsorSql = """
                INSERT INTO sponsors (id , sponsor_name, sponsor_email, amount , currency, custom_message , razorpay_payment_id)
                VALUES(?, ?, ?, ?, ?, ?, ?)
                """;

        jdbcClient.sql(insertSponsorSql)
                .params(sponsorId , name , email , amount , currency , message, paymentId)
                .update();

        String payloadJson = String.format(
                "{\"sponsorName\":\"%s\", \"amount\":%f, \"currency\":\"%s\", \"message\":\"%s\"}",
                name, amount , currency , message != null ? message : ""
        );

        // save to outbox events table (JSONB cast is critical for Postgres
        String insertOutboxSql = """
                Insert INTO outbox_events(id , aggregate_type, aggregate_id, event_type, payload, status)
                VALUES (?, 'SPONSOR', ?, 'SPONSOR_CREATED', ?::jsonb, 'PENDING')
                """;

        jdbcClient.sql(insertOutboxSql)
                .params(eventId , sponsorId.toString(), payloadJson)
                .update();

        log.info("Successfully committed Sponsor and OutboxEvent to PostgreSQL.");
    }
}
