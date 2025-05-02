package com.example.meditag.domain.member.dto.request;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GuardianRequestDTO {
    private String phoneNumber;
    private String relationship;
}