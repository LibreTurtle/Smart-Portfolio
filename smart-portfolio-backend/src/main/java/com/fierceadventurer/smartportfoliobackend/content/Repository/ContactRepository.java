package com.fierceadventurer.smartportfoliobackend.content.Repository;

import com.fierceadventurer.smartportfoliobackend.content.entity.ContactMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ContactRepository extends JpaRepository<ContactMessage , UUID> {
    List<ContactMessage> findByIsRealFalseOrderBySubmittedAtDesc();
}
