package com.example.meditag.domain.chatbot.service.message;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecordingPlayService {

    public boolean isApplicable(String message) {
        return message.matches(".*(녹음|주의사항|재생|들려줘).*");
    }

    public String execute(String username, String message) {
        // 녹음 재생 로직
        return "주의사항 녹음을 재생할게요.";
    }
}
