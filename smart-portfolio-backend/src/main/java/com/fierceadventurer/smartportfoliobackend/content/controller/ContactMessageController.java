package com.fierceadventurer.smartportfoliobackend.content.controller;

import com.fierceadventurer.smartportfoliobackend.content.dto.ContactMessageRequest;
import com.fierceadventurer.smartportfoliobackend.content.dto.ContactMessageResponse;
import com.fierceadventurer.smartportfoliobackend.content.service.ContactMessageService;
import com.fierceadventurer.smartportfoliobackend.content.service.ContactMessageServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/contact")
@RequiredArgsConstructor
public class ContactMessageController {
    private final ContactMessageService contactMessageService;

    @PostMapping
    public ResponseEntity<ContactMessageResponse> submitContactMessage(
            @Valid @RequestBody ContactMessageRequest request
            ){
        ContactMessageResponse response = contactMessageService.submitMessage(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
