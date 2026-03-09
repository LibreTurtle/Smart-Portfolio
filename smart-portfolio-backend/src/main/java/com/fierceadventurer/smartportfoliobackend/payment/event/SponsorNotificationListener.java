package com.fierceadventurer.smartportfoliobackend.payment.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SponsorNotificationListener {

    @Async
    @EventListener
    public void onSponsorPaid(SponsorPaidEvent event){
        log.info("======================================================");
        log.info("EVENT BUS INTERCEPT: New Sponsorship Processed!");
        log.info("Payload data: {}", event.payload());
        log.info("======================================================");
    }
}
