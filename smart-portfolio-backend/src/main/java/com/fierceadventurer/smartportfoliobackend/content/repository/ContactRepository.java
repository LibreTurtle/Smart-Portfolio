package com.fierceadventurer.smartportfoliobackend.content.repository;

import com.fierceadventurer.smartportfoliobackend.content.entity.ContactMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContactRepository extends JpaRepository<ContactMessage , UUID> {
    List<ContactMessage> findByIsReadFalseOrderBySubmittedAtDesc();
}
