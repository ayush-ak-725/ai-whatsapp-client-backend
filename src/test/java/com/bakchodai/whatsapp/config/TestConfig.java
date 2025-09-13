package com.bakchodai.whatsapp.config;

import com.bakchodai.whatsapp.domain.model.Character;
import com.bakchodai.whatsapp.domain.model.Group;
import com.bakchodai.whatsapp.domain.model.Message;
import com.bakchodai.whatsapp.domain.repository.CharacterRepository;
import com.bakchodai.whatsapp.domain.repository.GroupRepository;
import com.bakchodai.whatsapp.domain.repository.MessageRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Test configuration for integration tests
 * 
 * This configuration provides test-specific beans and settings
 * for integration testing scenarios.
 */
@TestConfiguration
@ActiveProfiles("test")
public class TestConfig {
    
    /**
     * Create test data for integration tests
     */
    @Bean
    @Primary
    public TestDataInitializer testDataInitializer(
            GroupRepository groupRepository,
            CharacterRepository characterRepository,
            MessageRepository messageRepository) {
        return new TestDataInitializer(groupRepository, characterRepository, messageRepository);
    }
    
    /**
     * Test data initializer class
     */
    public static class TestDataInitializer {
        
        private final GroupRepository groupRepository;
        private final CharacterRepository characterRepository;
        private final MessageRepository messageRepository;
        
        public TestDataInitializer(GroupRepository groupRepository,
                                 CharacterRepository characterRepository,
                                 MessageRepository messageRepository) {
            this.groupRepository = groupRepository;
            this.characterRepository = characterRepository;
            this.messageRepository = messageRepository;
        }
        
        public void initializeTestData() {
            // Create test characters
            Character virat = createTestCharacter("Virat Kohli", "Cricketer", "You are Virat Kohli, the legendary cricketer.");
            Character kapil = createTestCharacter("Kapil Sharma", "Comedian", "You are Kapil Sharma, the famous comedian.");
            Character elon = createTestCharacter("Elon Musk", "Tech CEO", "You are Elon Musk, the tech entrepreneur.");
            
            // Create test group
            Group testGroup = createTestGroup("Test Group", "A group for testing");
            
            // Add characters to group
            testGroup.addMember(virat);
            testGroup.addMember(kapil);
            testGroup.addMember(elon);
            
            // Save entities
            groupRepository.save(testGroup);
            
            // Create test messages
            createTestMessage(testGroup, virat, "Hello everyone! How are you doing?");
            createTestMessage(testGroup, kapil, "Hey Virat! Ready for some fun?");
            createTestMessage(testGroup, elon, "Interesting conversation happening here!");
        }
        
        private Character createTestCharacter(String name, String traits, String systemPrompt) {
            Character character = new Character();
            character.setName(name);
            character.setPersonalityTraits(traits);
            character.setSystemPrompt(systemPrompt);
            character.setSpeakingStyle("casual");
            character.setBackground("Test character for integration tests");
            character.setIsActive(true);
            character.setCreatedAt(LocalDateTime.now());
            return characterRepository.save(character);
        }
        
        private Group createTestGroup(String name, String description) {
            Group group = new Group();
            group.setName(name);
            group.setDescription(description);
            group.setIsActive(true);
            group.setCreatedAt(LocalDateTime.now());
            return groupRepository.save(group);
        }
        
        private Message createTestMessage(Group group, Character character, String content) {
            Message message = new Message();
            message.setGroup(group);
            message.setCharacter(character);
            message.setContent(content);
            message.setMessageType(Message.MessageType.TEXT);
            message.setTimestamp(LocalDateTime.now());
            message.setIsAiGenerated(true);
            message.setResponseTimeMs(1000L);
            return messageRepository.save(message);
        }
    }
}

