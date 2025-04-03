package com.example.meditag.domain.chatbot.repository;

import com.example.meditag.domain.chatbot.entity.FAQ;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FAQRepository extends JpaRepository<FAQ, Long> {
}

