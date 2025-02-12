package com.example.meditag.domain.member.mapper;

import com.example.meditag.domain.member.dto.request.RegisterDTO;
import com.example.meditag.domain.member.entity.Member;

public class MemberMapper {

    // Entity -> DTO
    public static RegisterDTO toRegisterDTO(Member member) {
        return RegisterDTO.builder()
                .username(member.getUsername())
                .name(member.getName())
                .phone(member.getPhone())
                .build();
    }
}
