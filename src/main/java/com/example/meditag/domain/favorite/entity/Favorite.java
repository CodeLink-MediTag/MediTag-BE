package com.example.meditag.domain.favorite.entity;

import com.example.meditag.domain.common.model.BaseTimeEntity;
import com.example.meditag.domain.medicine.entity.Medicine;
import com.example.meditag.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Favorite extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "member_id", unique = true)
    private Member member;

    @ManyToOne
    @JoinColumn(name = "medicine_id")
    private Medicine medicine;
}