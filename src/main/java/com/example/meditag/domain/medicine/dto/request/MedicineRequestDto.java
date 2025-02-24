package com.example.meditag.domain.medicine.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MedicineRequestDto {

    //약 이름
    @NotBlank(message = "약이름은 필수입니다.")
    private String name;

    //특징
    private String characteristic;

    //복용 시작 날짜
    @NotBlank(message = "복용 시작 날짜는 필수입니다.")
    private LocalDate startDate;

    //복용 기간
    @NotBlank(message = "복용기간 입력은 필수입니다.")
    private int duration;

    //복용 횟수(일반약 일때)
    @NotBlank(message = "복용횟수 등록은 필수입니다.")
    private int frequency;

    //사진
    private String imageUrl;

    //처방약 여부
    private boolean isPrescribed;
    //멤버 아이디
    private Long memberId;
}
