package com.example.meditag.domain.alarm.repository;

import com.example.meditag.domain.alarm.entity.Alarm;
import com.example.meditag.domain.medicine.entity.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AlarmRepository extends JpaRepository<Alarm, Long> {

    @Query("SELECT a FROM Alarm a " +
            "JOIN a.calendar c " +
            "JOIN c.medicine m " +
            "WHERE m = :medicine AND c.date = :date")
    List<Alarm> findByMedicineAndDate(@Param("medicine") Medicine medicine, @Param("date") LocalDate date);

    List<Alarm> findByAlarmTimeBetweenAndTakingFalse(LocalDateTime startTime, LocalDateTime endTime);
//    List<Alarm> findByMedicineIdAndAlarmTimeAfter(Long medicineId, LocalDateTime dateTime);
//    List<Alarm> findByMedicineId(Long medicineId);

    @Query("SELECT a FROM Alarm a " +
            "JOIN a.calendar c " +
            "JOIN c.medicine m " +
            "JOIN m.member mem " +
            "WHERE mem.username = :username")
    List<Alarm> findAllAlarmsByUsername(String username);

    // ✅ 추가된 메서드
    @Query("SELECT a FROM Alarm a " +
            "JOIN a.calendar c " +
            "JOIN c.medicine m " +
            "WHERE m.name = :name AND a.alarmTime = :time")
    Optional<Alarm> findByMedicineNameAndAlarmTime(@Param("name") String name, @Param("time") LocalTime time);

    @Query("SELECT a FROM Alarm a WHERE a.calendar.medicine.member.username = :username AND a.dosageTime = :dosageTime")
    List<Alarm> findByUsernameAndDosageTime(@Param("username") String username, @Param("dosageTime") String dosageTime);

    @Query("SELECT a FROM Alarm a WHERE a.calendar.medicine.member.username = :username AND a.alarmTime BETWEEN :startDateTime AND :endDateTime")
    List<Alarm> findByUsernameAndAlarmDate(@Param("username") String username,
                                           @Param("startDateTime") LocalDateTime startDateTime,
                                           @Param("endDateTime") LocalDateTime endDateTime);

    @Query("SELECT a FROM Alarm a WHERE a.calendar.medicine.member.username = :username AND a.dosageTime = :dosageTime AND a.alarmTime BETWEEN :startDateTime AND :endDateTime")
    List<Alarm> findByUsernameAndDosageTimeAndDate(@Param("username") String username,
                                                   @Param("dosageTime") String dosageTime,
                                                   @Param("startDateTime") LocalDateTime startDateTime,
                                                   @Param("endDateTime") LocalDateTime endDateTime);

    List<Alarm> findByCalendar_Medicine_Member_Username(String username);
}
