package com.example.meditag.domain.alarm.repository;

import com.example.meditag.domain.alarm.entity.Alarm;
import com.example.meditag.domain.medicine.entity.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlarmRepository extends JpaRepository<Alarm, Long> {
    List<Alarm> findByMedicine(Medicine medicine);
}
