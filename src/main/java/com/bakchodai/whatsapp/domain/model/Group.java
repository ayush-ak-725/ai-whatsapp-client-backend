package com.bakchodai.whatsapp.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Entity representing a WhatsApp-style group chat
 * 
 * A group contains multiple AI characters that can participate in conversations.
 * Each group has a unique identifier and maintains relationships with its members.
 */
@Entity
@Table(name = "groups")
public class Group {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @NotBlank(message = "Group name is required")
    @Size(max = 255, message = "Group name must not exceed 255 characters")
    @Column(nullable = false)
    private String name;
    
    @Column(name = "description", length = 1000)
    private String description;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<GroupMember> members = new HashSet<>();
    
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Message> messages = new HashSet<>();
    
    // Constructors
    public Group() {}
    
    public Group(String name, String description) {
        this.name = name;
        this.description = description;
    }
    
    // Getters and Setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public Set<GroupMember> getMembers() {
        return members;
    }
    
    public void setMembers(Set<GroupMember> members) {
        this.members = members;
    }
    
    public Set<Message> getMessages() {
        return messages;
    }
    
    public void setMessages(Set<Message> messages) {
        this.messages = messages;
    }
    
    // Business methods
    public void addMember(Character character) {
        GroupMember member = new GroupMember(this, character);
        this.members.add(member);
    }
    
    public void removeMember(Character character) {
        this.members.removeIf(member -> member.getCharacter().equals(character));
    }
    
    public boolean hasMember(Character character) {
        return this.members.stream()
                .anyMatch(member -> member.getCharacter().equals(character));
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Group group = (Group) o;
        return id != null && id.equals(group.id);
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
    
    @Override
    public String toString() {
        return "Group{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", isActive=" + isActive +
                ", createdAt=" + createdAt +
                '}';
    }
}


