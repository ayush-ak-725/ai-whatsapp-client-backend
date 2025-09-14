package com.bakchodai.whatsapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for Bakchod AI WhatsApp Backend
 * 
 * This application provides a backend service for managing AI-powered WhatsApp group chats
 * with celebrity characters that can engage in natural conversations.
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class WhatsappAiBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(WhatsappAiBackendApplication.class, args);
    }
}


