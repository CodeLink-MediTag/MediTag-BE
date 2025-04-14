package com.example.meditag.domain.chatbot.service.message.register;

import com.example.meditag.domain.chatbot.dto.MedicineRegisterSession;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class SessionStorage {
    private final Map<String, MedicineRegisterSession> sessionMap = new HashMap<>();

    public MedicineRegisterSession getOrCreateSession(String username) {
        return sessionMap.computeIfAbsent(username, u -> {
            MedicineRegisterSession session = new MedicineRegisterSession();
            return session;
        });
    }

    public void clearSession(String username) {
        sessionMap.remove(username);
    }
}
