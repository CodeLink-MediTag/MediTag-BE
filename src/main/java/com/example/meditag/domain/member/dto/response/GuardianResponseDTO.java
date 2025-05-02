package com.example.meditag.domain.member.dto.response;
import com.example.meditag.domain.member.entity.Guardian;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GuardianResponseDTO {
    private Long id;
    private String phoneNumber;
    private String relationship;

    public static GuardianResponseDTO from(Guardian guardian) {
        return GuardianResponseDTO.builder()
                .id(guardian.getId())
                .phoneNumber(guardian.getPhoneNumber())
                .relationship(guardian.getRelationship())
                .build();
    }
}