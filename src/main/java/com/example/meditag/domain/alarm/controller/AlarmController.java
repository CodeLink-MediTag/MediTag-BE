package com.example.meditag.domain.alarm.controller;

import com.example.meditag.domain.alarm.service.AlarmService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping
public class AlarmController {

    private final AlarmService alarmService;

}
