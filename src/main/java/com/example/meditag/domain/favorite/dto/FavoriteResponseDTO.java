package com.example.meditag.domain.favorite.dto;

import lombok.*;

@Getter @AllArgsConstructor @NoArgsConstructor @Builder
public class FavoriteResponseDTO {
    private Long medicineId;
    private String medicineName;
    private String characteristic;
    private String imageUrl;
    private Boolean prescribed;
}