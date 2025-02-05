package com.example.meditag.domain.member.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RegisterDTO {

    private String email;
    private String name;
    private String phone;
    private String password;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
