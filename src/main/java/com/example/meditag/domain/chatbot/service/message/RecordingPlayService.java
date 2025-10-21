package com.example.meditag.domain.chatbot.service.message;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecordingPlayService {

    public boolean isApplicable(String message) {
        if (message == null || message.trim().isEmpty()) return false;
        String m = message.replaceAll("\\s+", "").toLowerCase();
        // 녹음/재생/들려줘 키워드가 있는 경우 즉시 true
        boolean hasPlayKeyword = m.contains("녹음") || m.contains("재생") || m.contains("들려줘");
        // 주의사항 요청과 함께 재생 또는 들려줘/듣기 요청이 있는 경우에만 true
        boolean hasCaution = m.contains("주의사항") || m.contains("주의");
        boolean hasAudioRequest = m.contains("재생") || m.contains("들려줘") || m.contains("듣");
        return hasPlayKeyword || (hasCaution && hasAudioRequest);
    }

    public String execute(String username, String message) {
        // 녹음 재생 로직
        return "주의사항 녹음을 재생할게요.";
    }
}
