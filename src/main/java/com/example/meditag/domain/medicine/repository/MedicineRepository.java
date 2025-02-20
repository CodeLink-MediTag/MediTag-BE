package com.example.meditag.domain.medicine.repository;

import com.example.meditag.domain.medicine.entity.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MedicineRepository extends JpaRepository<Medicine, Long> {
}
