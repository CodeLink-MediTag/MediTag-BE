package com.example.meditag.domain.chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDTO {
    private String sender;
    private String content;
    private ActionDTO action;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActionDTO {
        private ActionType type;
        private String target;

        public enum ActionType {
            NONE, NAVIGATE, FETCH_DATA
        }
    }
}
