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

@Service // Spring 서비스 컴포넌트로 등록
@RequiredArgsConstructor // final 필드를 가진 생성자 자동 생성
public class ChatSessionService {

    private final ChatSessionRepository chatSessionRepository; // 채팅 세션 저장소 의존성 주입
    private final MemberRepository memberRepository; // 회원 저장소 의존성 주입

    public List<ChatSessionDTO> getSessionsByMemberId(String username) {
        Member member = memberRepository.findByUsername(username) // 사용자 이름으로 회원 조회
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND)); // 없으면 예외 발생

        return chatSessionRepository.findByMemberId(member.getId()) // 회원 ID로 채팅 세션 목록 조회
                .stream()
                .map(session -> ChatSessionDTO.builder() // DTO로 변환
                        .id(session.getId()) // 세션 ID
                        .startedAt(session.getStartedAt()) // 시작 시간
                        .endedAt(session.getEndedAt()) // 종료 시간
                        .memberId(session.getMember().getId()) // 회원 ID
                        .build())
                .collect(Collectors.toList()); // 리스트로 반환
    }

    public ChatSessionDTO createChatSession(String username) {
        Member member = memberRepository.findByUsername(username) // 사용자 이름으로 회원 조회
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND)); // 없으면 예외

        ChatSession session = ChatSession.builder() // 새 채팅 세션 생성
                .startedAt(LocalDateTime.now()) // 현재 시간으로 시작 시간 설정
                .endedAt(null) // 종료 시간은 아직 없음
                .member(Member.builder().id(member.getId()).build()) // 회원 정보 설정 (ID만)
                .build();

        ChatSession saved = chatSessionRepository.save(session); // 세션 저장

        return ChatSessionDTO.builder() // 저장된 세션 정보를 DTO로 반환
                .id(saved.getId())
                .startedAt(saved.getStartedAt())
                .endedAt(saved.getEndedAt())
                .memberId(saved.getMember().getId())
                .build();
    }
}
