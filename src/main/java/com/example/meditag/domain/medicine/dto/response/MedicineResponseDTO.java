package com.example.meditag.domain.medicine.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MedicineResponseDTO {

    private Long id;

    //약 이름
    private String name;

    //특징
    private String characteristic;

    //복용 시작 날짜
    private LocalDate startDate;

    //복용 기간
    private Integer duration;

    //복용 횟수(일반약 일때)
    private Integer frequency;

    //사진
    private String imageUrl;

    //처방약 여부
    private Boolean prescribed;
}
