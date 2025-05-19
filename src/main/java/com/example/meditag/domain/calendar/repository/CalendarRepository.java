package com.example.meditag.domain.calendar.repository;

import com.example.meditag.domain.calendar.entity.Calendar;
import com.example.meditag.domain.medicine.entity.Medicine;
import com.example.meditag.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CalendarRepository extends JpaRepository<Calendar, Long> {
    @Query("SELECT c FROM Calendar c WHERE c.medicine.member = :member AND c.date = :date")
    List<Calendar> findByMemberAndDate(@Param("member") Member member, @Param("date") LocalDate date);

    List<Calendar> findByMedicine_Member_Username(String username);

    Optional<Calendar> findByMedicineAndDate(Medicine medicine, LocalDate date);

    List<Calendar> findByMedicine(Medicine medicine);
}
