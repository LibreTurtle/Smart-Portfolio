package com.fierceadventurer.smartportfoliobackend.content.service;

import com.fierceadventurer.smartportfoliobackend.content.dto.ContactMessageRequest;
import com.fierceadventurer.smartportfoliobackend.content.dto.ContactMessageResponse;
import com.fierceadventurer.smartportfoliobackend.content.mapper.ContactMessageMapper;
import com.fierceadventurer.smartportfoliobackend.content.repository.ContactRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContactMessageServiceImpl  implements ContactMessageService{

    private final ContactRepository repository;
    private final ContactMessageMapper mapper;
    private final DiscordNotificationService discordNotificationService

    @Override
    public ContactMessageResponse submitMessage(ContactMessageRequest request) {
        log.info("Recived new contact message form : {}", request.senderEmail());

        var entity = mapper.toEntity(request);

        var savedEntity = repository.save(entity);

        discordNotificationService.sendContactNotification(request);

        return mapper.toResponse(savedEntity);
    }
}
