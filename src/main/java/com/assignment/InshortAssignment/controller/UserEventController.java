package com.assignment.InshortAssignment.controller;

import com.assignment.InshortAssignment.model.UserEvent;
import com.assignment.InshortAssignment.repository.UserEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events")
public class UserEventController {

    private final UserEventRepository userEventRepository;

    @Autowired
    public UserEventController(UserEventRepository userEventRepository) {
        this.userEventRepository = userEventRepository;
    }

    @PostMapping
    public ResponseEntity<UserEvent> recordEvent(@RequestBody UserEvent event) {
        // Set current time if not provided
        if (event.getEventTime() == null) {
            event.setEventTime(LocalDateTime.now());
        }

        UserEvent savedEvent = userEventRepository.save(event);
        return ResponseEntity.ok(savedEvent);
    }

    @GetMapping("/article/{articleId}")
    public ResponseEntity<Map<String, Object>> getEventsByArticle(@PathVariable UUID articleId) {
        Map<String, Object> response = new HashMap<>();
        response.put("events", userEventRepository.findByArticleId(articleId));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getEventsByUser(@PathVariable String userId) {
        Map<String, Object> response = new HashMap<>();
        response.put("events", userEventRepository.findByUserId(userId));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/recent")
    public ResponseEntity<Map<String, Object>> getRecentEvents(
            @RequestParam(required = false, defaultValue = "24") Integer hours) {

        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(hours);

        Map<String, Object> response = new HashMap<>();
        response.put("events", userEventRepository.findByEventTimeAfter(cutoffTime));
        return ResponseEntity.ok(response);
    }
}
