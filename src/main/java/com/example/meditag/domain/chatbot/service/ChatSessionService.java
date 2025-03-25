package com.example.meditag.domain.chatbot.service;

import com.example.meditag.domain.chatbot.dto.ChatSessionDTO;
import com.example.meditag.domain.chatbot.entity.ChatSession;
import com.example.meditag.domain.chatbot.repository.ChatSessionRepository;
import com.example.meditag.domain.member.entity.Member;
import com.example.meditag.domain.member.repository.MemberRepository;
import com.example.meditag.global.error.exception.CustomException;
import com.example.meditag.global.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatSessionService {

    private final ChatSessionRepository chatSessionRepository;
    private final MemberRepository memberRepository;

    public List<ChatSessionDTO> getSessionsByMemberId(String username) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        return chatSessionRepository.findByMemberId(member.getId())
                .stream()
                .map(session -> ChatSessionDTO.builder()
                        .id(session.getId())
                        .startedAt(session.getStartedAt())
                        .endedAt(session.getEndedAt())
                        .memberId(session.getMember().getId())
                        .build())
                .collect(Collectors.toList());
    }

    public ChatSessionDTO createChatSession(String username) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        ChatSession session = ChatSession.builder()
                .startedAt(LocalDateTime.now())
                .endedAt(null)
                .member(Member.builder().id(member.getId()).build())
                .build();

        ChatSession saved = chatSessionRepository.save(session);

        return ChatSessionDTO.builder()
                .id(saved.getId())
                .startedAt(saved.getStartedAt())
                .endedAt(saved.getEndedAt())
                .memberId(saved.getMember().getId())
                .build();
    }
}

