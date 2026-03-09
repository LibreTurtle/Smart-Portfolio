package com.fierceadventurer.smartportfoliobackend.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentModuleService {

    private final JdbcClient jdbcClient;

    @Transactional
    public void processSuccessfulSponsorship(String razorpayEventId, String paymentId , String name , String email ,
                                             double amount, String currency){

        UUID sponsorId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        log.info("Processing verified Razorpay payment. Initiating Outbox transaction for Sponsor: {}", email);

        // save to sponsor table
        String insertSponsorSql = """
                INSERT INTO sponsors (id , sponsor_name, sponsor_email, amount , currency, status , razorpay_payment_id)
                VALUES(?, ?, ?, ?, ?, 'SUCCESS', ?)
                """;

        jdbcClient.sql(insertSponsorSql)
                .params(sponsorId , name , email , amount , currency , paymentId)
                .update();

        String payloadJson = String.format(
                "{\"sponsorName\":\"%s\", \"amount\":%f, \"currency\":\"%s\", \"email\":\"%s\"}",
                name, amount , currency , email
        );

        // save to outbox events table (JSONB cast is critical for Postgres
        String insertOutboxSql = """
                Insert INTO outbox_events(id , aggregate_type, aggregate_id, event_type, payload, is_Processed, event_id)
                VALUES (?, 'SPONSOR', ?, 'SPONSOR_CREATED', ?::jsonb, 'PENDING')
                """;

        jdbcClient.sql(insertOutboxSql)
                .params(eventId , sponsorId.toString(), payloadJson, razorpayEventId)
                .update();

        log.info("Successfully committed Sponsor and OutboxEvent to PostgreSQL.");
    }
}
