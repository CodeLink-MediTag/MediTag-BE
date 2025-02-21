package com.example.meditag.domain.auth.service;

import com.example.meditag.domain.auth.dto.CustomUserDetails;
import com.example.meditag.domain.member.entity.Member;
import com.example.meditag.domain.member.repository.MemberRepository;
import com.example.meditag.global.error.exception.CustomException;
import com.example.meditag.global.error.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository; // 사용자 정보를 조회하기 위한 MemberRepository

    // 생성자를 통해 MemberRepository 의존성 주입 (Dependency Injection)
    public CustomUserDetailsService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
        log.info("[CustomUserDetailsService] CustomUserDetailsService 생성자 주입");
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        log.info("[CustomUserDetailsService/loadUserByUsername] 1. Member 정보를 CustomUserDetails 객체로 변환하여 반환, username: {}", username);

        // 조회된 Member 정보를 CustomUserDetails 객체로 변환하여 반환
        return new CustomUserDetails(member);
    }
}
