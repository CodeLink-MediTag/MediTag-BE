package com.example.meditag.domain.alarm.repository;

import com.example.meditag.domain.alarm.entity.Alarm;
import com.example.meditag.domain.calendar.entity.Calendar;
import com.example.meditag.domain.medicine.entity.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AlarmRepository extends JpaRepository<Alarm, Long> {

    /* ===== 기존 메서드 유지 ===== */

    @Query("""
        SELECT a FROM Alarm a
        JOIN a.calendar c
        JOIN c.medicine m
        WHERE m = :medicine AND c.date = :date
    """)
    List<Alarm> findByMedicineAndDate(@Param("medicine") Medicine medicine,
                                      @Param("date") LocalDate date);

    @Query("""
        SELECT a FROM Alarm a
        WHERE a.calendar.medicine.member.username = :username
          AND a.alarmTime BETWEEN :startDateTime AND :endDateTime
    """)
    List<Alarm> findByUsernameAndAlarmDate(@Param("username") String username,
                                           @Param("startDateTime") LocalDateTime startDateTime,
                                           @Param("endDateTime") LocalDateTime endDateTime);

    @Query("""
        SELECT a FROM Alarm a
        WHERE a.calendar = :calendar AND a.dosageTime = :dosageTime
    """)
    Optional<Alarm> findByCalendarAndDosageTime(@Param("calendar") Calendar calendar,
                                                @Param("dosageTime") String dosageTime);

    @Query("""
        SELECT a FROM Alarm a
        WHERE a.calendar = :calendar AND a.alarmTime = :alarmTime
    """)
    Optional<Alarm> findByCalendarAndAlarmTime(@Param("calendar") Calendar calendar,
                                               @Param("alarmTime") LocalDateTime alarmTime);

    List<Alarm> findByCalendar_Medicine_Member_Username(String username);


    // 날짜/시간 범위(사용자 전체)
    @Query("""
        SELECT a FROM Alarm a
        JOIN a.calendar c
        JOIN c.medicine m
        JOIN m.member mem
        WHERE mem.username = :username
          AND a.alarmTime >= :startDateTime
          AND a.alarmTime <= :endDateTime
        ORDER BY a.alarmTime ASC
    """)
    List<Alarm> findByUsernameAndDateTimeRange(@Param("username") String username,
                                               @Param("startDateTime") LocalDateTime startDateTime,
                                               @Param("endDateTime") LocalDateTime endDateTime);

    // 약 이름 + 날짜/시간 범위
    @Query("""
        SELECT a FROM Alarm a
        JOIN a.calendar c
        JOIN c.medicine m
        JOIN m.member mem
        WHERE mem.username = :username
          AND m.name = :medicineName
          AND a.alarmTime >= :start
          AND a.alarmTime <= :end
        ORDER BY a.alarmTime ASC
    """)
    List<Alarm> findByUsernameAndMedicineNameAndAlarmDate(@Param("username") String username,
                                                          @Param("medicineName") String medicineName,
                                                          @Param("start") LocalDateTime start,
                                                          @Param("end") LocalDateTime end);


    /* ===== 여기부터 추가: 서비스에서 요구하는 공용 조회 메서드들 ===== */

    // 1) 날짜로 조회 (정렬 포함)
    @Query("""
        SELECT a FROM Alarm a
        WHERE a.calendar.date = :date
        ORDER BY a.alarmTime ASC
    """)
    List<Alarm> findByDateOrderByAlarmTimeAsc(@Param("date") LocalDate date);

    // 1-1) 날짜로 조회 (정렬 없이 — 필요 시 서비스에서 정렬)
    @Query("""
        SELECT a FROM Alarm a
        WHERE a.calendar.date = :date
    """)
    List<Alarm> findByDate(@Param("date") LocalDate date);

    // 2) 시간 범위 조회 (정렬 포함)
    @Query("""
        SELECT a FROM Alarm a
        WHERE a.alarmTime BETWEEN :start AND :end
        ORDER BY a.alarmTime ASC
    """)
    List<Alarm> findByAlarmTimeBetweenOrderByAlarmTimeAsc(@Param("start") LocalDateTime start,
                                                          @Param("end") LocalDateTime end);

    // 2-1) 시간 범위 조회 (정렬 없이 — 필요 시 서비스에서 정렬)
    @Query("""
        SELECT a FROM Alarm a
        WHERE a.alarmTime BETWEEN :start AND :end
    """)
    List<Alarm> findByAlarmTimeBetween(@Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end);
}
