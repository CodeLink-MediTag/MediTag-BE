package com.example.meditag.domain.member.service;

import com.example.meditag.domain.member.dto.request.RegisterDTO;
import com.example.meditag.domain.member.entity.Member;
import com.example.meditag.domain.member.mapper.MemberMapper;
import com.example.meditag.domain.member.repository.MemberRepository;
import com.example.meditag.global.error.exception.CustomException;
import com.example.meditag.global.error.exception.ErrorCode;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterService {

    private final MemberRepository memberRepository;  // 사용자 정보를 저장할 저장소 (Repository)
    private final BCryptPasswordEncoder bCryptPasswordEncoder;  // 비밀번호 암호화를 위한 BCryptPasswordEncoder

    public RegisterService(MemberRepository memberRepository, BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.memberRepository = memberRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

    // 회원가입 API
    @Transactional
    public RegisterDTO registerProcess(RegisterDTO registerDTO) {
        String username = registerDTO.getUsername();
        String password = registerDTO.getPassword();

        // 이메일 중복 확인 (Optional + orElseThrow 사용)
        memberRepository.findByUsername(username)
                .ifPresent(member -> {
                    throw new CustomException(ErrorCode.MEMBER_ALREADY_EXISTS);
                });

        // 회원 객체 생성 (Builder 패턴 사용)
        Member member = Member.builder()
                .username(registerDTO.getUsername())
                .name(registerDTO.getName())
                .phone(registerDTO.getPhone())
                .password(bCryptPasswordEncoder.encode(password))
                .firebasetoken(registerDTO.getFirebasetoken())
                .role("ROLE_USER")  // 기본 역할 설정
                .build();

        // 저장
        memberRepository.save(member);

        return MemberMapper.toRegisterDTO(member);
    }
}