package com.example.meditag.domain.oauth2.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
@Controller
public class MainController {
    @GetMapping("/")
    public String mainPage() {

        return "main";
    }
    @GetMapping("/reminder")
    public String reminderpage(){
        return "alarmregist";
    }

}
