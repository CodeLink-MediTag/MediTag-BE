package com.example.meditag.domain.favorite.repository;


import com.example.meditag.domain.favorite.entity.Favorite;
import com.example.meditag.domain.medicine.entity.Medicine;
import com.example.meditag.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    Optional<Favorite> findByMember(Member member);
    void deleteByMember(Member member);
    void deleteByMedicine(Medicine medicine);
    List<Favorite> findAll();
}
