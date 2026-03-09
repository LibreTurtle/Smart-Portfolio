package com.fierceadventurer.smartportfoliobackend.payment.scheduler;

import com.fierceadventurer.smartportfoliobackend.payment.event.SponsorPaidEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPollingWorker {

    private final JdbcClient jdbcClient;
    private final ApplicationEventPublisher eventPublisher;

    // fixedDelay = 10000 ensures it waits 10 seconds AFTER the last run finishes
    @Scheduled(fixedDelay = 10000)
    public void pollAndProcessOutboxEvents(){

        //fetch up to 50 unproccessed events
        String fetchPendingSql = """
                SELECT id, event_type, payload
                FROM outbox_events
                WHERE is_processed = false
                ORDER BY created_at ASC
                LIMIT 50
                """;

        List<Map<String , Object>> pendingEvents = jdbcClient.sql(fetchPendingSql)
                .query()
                .listOfRows();

        if(pendingEvents.isEmpty()){
            return; // sleep instantly when no work to do
        }

        log.info("Outbox Poller woke up: Found {} pending events.", pendingEvents.size());

        // iterating through events processing them one by one
        for(Map<String , Object> eventRow: pendingEvents){
            //extarct safely postgres UUID and JSONB types returned as objects so stringfy them
            String eventIdStr = eventRow.get("id").toString();
            String eventType = (String) eventRow.get("event_type");
            String payload = eventRow.get("payload").toString();

            try{
                //publish to springs internal event bus based on event type
                if("SPONSOR_CREATED".equals(eventType)){
                    eventPublisher.publishEvent(new SponsorPaidEvent(payload));
                }

                // Mark the specific event as processed
                String markProcessedSql = """
                        UPDATE outbox_events
                        SET is_processed = true
                        WHERE id = ?
                        """;

                jdbcClient.sql(markProcessedSql)
                        .param(UUID.fromString(eventIdStr))
                        .update();

                log.debug("Successfully published and resolved outbox event ID: {}", eventIdStr);
            }

            catch (Exception e){
                 // If one event fails to publish, the loop continues to the next event safely.
                log.error("Failed to process outbox event ID: {}. It will remain pending.", eventIdStr, e);
            }
        }
    }
}
