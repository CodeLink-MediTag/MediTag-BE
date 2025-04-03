package com.example.meditag.domain.chatbot.repository;

import com.example.meditag.domain.chatbot.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    List<ChatSession> findByMemberId(Long memberId); // 특정 사용자의 세션 조회
}
