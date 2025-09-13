package com.bakchodai.whatsapp.ai.model;

import com.bakchodai.whatsapp.domain.model.Character;
import com.bakchodai.whatsapp.domain.model.Group;
import com.bakchodai.whatsapp.domain.model.Message;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Context object containing all information needed for AI response generation
 * 
 * This class encapsulates the conversation state, character information,
 * and group context required for generating appropriate AI responses.
 */
public class ConversationContext {
    
    private Group group;
    private Character currentCharacter;
    private List<Message> recentMessages;
    private List<Character> activeCharacters;
    private Map<String, Object> additionalContext;
    private LocalDateTime conversationStartTime;
    private String currentTopic;
    private ConversationMood mood;
    
    // Constructors
    public ConversationContext() {}
    
    public ConversationContext(Group group, Character currentCharacter, List<Message> recentMessages) {
        this.group = group;
        this.currentCharacter = currentCharacter;
        this.recentMessages = recentMessages;
    }
    
    // Getters and Setters
    public Group getGroup() {
        return group;
    }
    
    public void setGroup(Group group) {
        this.group = group;
    }
    
    public Character getCurrentCharacter() {
        return currentCharacter;
    }
    
    public void setCurrentCharacter(Character currentCharacter) {
        this.currentCharacter = currentCharacter;
    }
    
    public List<Message> getRecentMessages() {
        return recentMessages;
    }
    
    public void setRecentMessages(List<Message> recentMessages) {
        this.recentMessages = recentMessages;
    }
    
    public List<Character> getActiveCharacters() {
        return activeCharacters;
    }
    
    public void setActiveCharacters(List<Character> activeCharacters) {
        this.activeCharacters = activeCharacters;
    }
    
    public Map<String, Object> getAdditionalContext() {
        return additionalContext;
    }
    
    public void setAdditionalContext(Map<String, Object> additionalContext) {
        this.additionalContext = additionalContext;
    }
    
    public LocalDateTime getConversationStartTime() {
        return conversationStartTime;
    }
    
    public void setConversationStartTime(LocalDateTime conversationStartTime) {
        this.conversationStartTime = conversationStartTime;
    }
    
    public String getCurrentTopic() {
        return currentTopic;
    }
    
    public void setCurrentTopic(String currentTopic) {
        this.currentTopic = currentTopic;
    }
    
    public ConversationMood getMood() {
        return mood;
    }
    
    public void setMood(ConversationMood mood) {
        this.mood = mood;
    }
    
    /**
     * Enum representing the current mood of the conversation
     */
    public enum ConversationMood {
        CASUAL,
        FORMAL,
        HUMOROUS,
        SERIOUS,
        EXCITED,
        CALM,
        DEBATE,
        GOSSIP,
        PLANNING
    }
    
    @Override
    public String toString() {
        return "ConversationContext{" +
                "group=" + (group != null ? group.getName() : "null") +
                ", currentCharacter=" + (currentCharacter != null ? currentCharacter.getName() : "null") +
                ", recentMessagesCount=" + (recentMessages != null ? recentMessages.size() : 0) +
                ", activeCharactersCount=" + (activeCharacters != null ? activeCharacters.size() : 0) +
                ", currentTopic='" + currentTopic + '\'' +
                ", mood=" + mood +
                '}';
    }
}

