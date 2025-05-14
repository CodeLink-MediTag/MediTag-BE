package com.example.meditag.domain.medicine.repository;

import com.example.meditag.domain.medicine.entity.Medicine;
import com.example.meditag.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicineRepository extends JpaRepository<Medicine, Long> {
    boolean existsByMemberAndPrescribedTrue(Member member);

    boolean existsByMemberAndPrescribed(Member member, boolean prescribed);

    List<Medicine> findByMember_Username(String username);

    Optional<Medicine> findByIdAndMember(Long id, Member member);

}
