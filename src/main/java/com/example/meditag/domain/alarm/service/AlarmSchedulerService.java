package com.example.meditag.domain.alarm.service;

import com.example.meditag.domain.alarm.entity.Alarm;
import com.example.meditag.domain.alarm.repository.AlarmRepository;
import com.example.meditag.domain.medicine.entity.Medicine;
import com.example.meditag.domain.member.entity.Member;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Service
@EnableScheduling
@EnableAsync
@RequiredArgsConstructor
public class AlarmSchedulerService {
    private final AlarmRepository alarmRepository;
    private final FCMService fcmService;
    private final TaskScheduler taskScheduler;

    private  final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    @Transactional
    public void scheduleAlarms(Medicine medicine, List<Alarm> alarms) {
        Member member = medicine.getMember();
        String token = member.getFirebasetoken();

        if(token == null || token.isEmpty()) {
            log.error("파이어베이스token is empty");
            return;
        }
        //enddate계산
        LocalDate endDate = medicine.getStartDate().plusDays(medicine.getDuration()-1);

        for (Alarm alarm : alarms) {
            scheduleAlarm(alarm, medicine,token, endDate);
        }
        //종료
        scheduleAlarmTermination(medicine.getId(), endDate);

    }
    private void scheduleAlarm(Alarm alarm, Medicine medicine, String token, LocalDate endDate) {
        LocalDateTime alarmTime = alarm.getAlarmTime();
        //과거 시간or 종료일 넘은 거 스케줄링 x
        if (alarmTime.isBefore(LocalDateTime.now()) ||
            alarmTime.toLocalDate().isAfter(endDate)){
            log.info("알림 시간이 유효 하지 않습니다");
            return;

        }
        ScheduledFuture<?> scheduledTask = taskScheduler.schedule(()->{
            try{
                if(!alarm.isTaking()){
                    String title = "약 복용 알림";
                    String body =String.format("%s 약 드실 시간이에요! 약 드셨나요?",
                             medicine.getName());
                    //파이어 베이스에 알림 전송
                    fcmService.sendPushNotification(token,title,body);

                    log.info("[AlarmSchedulerService] 알림 전송 완료 - 약: {}, 시간: {}",
                            medicine.getName(), alarm.getAlarmTime());

                }
            } catch (Exception e) {
                log.error(e.getMessage(),"알림 전송 실패 {}",medicine.getName());
            }
        },alarm.getAlarmTime().atZone(ZoneId.systemDefault()).toInstant());
        scheduledTasks.put(alarm.getId(), scheduledTask);
        log.info("[AlarmSchedulerService] 알림 예약 완료 - 약: {}, 시간: {}, 종료일: {}",
                medicine.getName(), alarm.getAlarmTime(), endDate);
    }

    private void scheduleAlarmTermination(long medicineId, LocalDate endDate) {
        LocalDateTime terminationTime= endDate.plusDays(1).atStartOfDay();

        taskScheduler.schedule(() -> {
            scheduledTasks.forEach((alarmId, scheduledTask) -> {
                cancelAlarm(alarmId);
            });

            log.info("[AlarmSchedulerService] 약(ID: {})의 복용 기간이 종료되어 알림이 모두 취소되었습니다.",
                    medicineId);
        }, terminationTime.atZone(ZoneId.systemDefault()).toInstant());

    }
    //특정 알림 취소
    public void cancelAlarm(long alarmId) {
        ScheduledFuture<?> scheduledTask = scheduledTasks.remove(alarmId);
        if(scheduledTask != null) {
            scheduledTask.cancel(false);
            log.info("[AlarmSchedulerService] 알림 취소 완료 - 알림 ID: {}", alarmId);
        }
    }
    //특정 약 모든 알람 취소 로직( 가져다 쓰면 알람 없어짐 )
//    public void cancelMedicineAlarms(Long medicineId) {
//        List<Alarm> alarms = alarmRepository.findByMedicineId(medicineId); // 이거 잘못됨
//        for (Alarm alarm : alarms) {
//            cancelAlarm(alarm.getId());
//        }
//        log.info("[AlarmSchedulerService] 약(ID: {})의 모든 알림이 취소되었습니다.", medicineId);
//    }
}
