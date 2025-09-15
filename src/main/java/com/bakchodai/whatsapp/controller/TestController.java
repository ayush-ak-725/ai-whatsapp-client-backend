package com.bakchodai.whatsapp.controller;

import com.bakchodai.whatsapp.service.ConversationEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/test")
public class TestController {
    
    @Autowired
    private ConversationEngine conversationEngine;
    
    @PostMapping("/conversation/{groupId}/trigger-turn")
    public String triggerConversationTurn(@PathVariable UUID groupId) {
        try {
            conversationEngine.processConversationTurn(groupId);
            return "Conversation turn triggered for group: " + groupId;
        } catch (Exception e) {
            return "Error triggering conversation turn: " + e.getMessage();
        }
    }
}
