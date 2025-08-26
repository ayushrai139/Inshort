package com.assignment.InshortAssignment.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "article_id", nullable = false)
    private UUID articleId;

    @Column(name = "event_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private EventType eventType;

    @Column(name = "event_time", nullable = false)
    private LocalDateTime eventTime;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    public enum EventType {
        VIEW, CLICK, SHARE, BOOKMARK
    }
}
