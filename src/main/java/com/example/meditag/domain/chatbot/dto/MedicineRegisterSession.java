package com.example.meditag.domain.chatbot.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class MedicineRegisterSession {
    private String name;
    private String characteristic;
    private LocalDate startDate;
    private int duration;
    private Integer frequency;
    private boolean prescribed;
    private Step currentStep;
    private String dosageTime;
    private LocalDateTime alarmTime;
    private String[] dosageTimes;



    public enum Step {
        NAME, CHARACTERISTIC, START_DATE, DURATION, FREQUENCY, PRESCRIBED,
        ALARM_DOSAGE_TIME, ALARM_TIME, COMPLETE
    }

}
