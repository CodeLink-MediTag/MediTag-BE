package com.example.meditag.domain.medicine_calendar.repository;

import com.example.meditag.domain.medicine_calendar.entity.MedicineCalendar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MedicineCalendarRepository extends JpaRepository<MedicineCalendar, Long> {
}
