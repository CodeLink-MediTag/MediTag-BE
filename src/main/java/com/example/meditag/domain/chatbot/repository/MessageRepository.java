package com.example.meditag.domain.chatbot.repository;

import com.example.meditag.domain.chatbot.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByChatSessionId(Long chatSessionId); // 특정 세션의 메시지 조회
}