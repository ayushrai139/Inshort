package com.assignment.InshortAssignment.repository;

import com.assignment.InshortAssignment.model.UserEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface UserEventRepository extends JpaRepository<UserEvent, UUID> {

    // Find events by article ID
    List<UserEvent> findByArticleId(UUID articleId);

    // Find events by user ID
    List<UserEvent> findByUserId(String userId);

    // Find recent events within a time window
    List<UserEvent> findByEventTimeAfter(LocalDateTime cutoffTime);

    // Find events by type
    List<UserEvent> findByEventType(UserEvent.EventType eventType);

    // Count events by article ID
    @Query("SELECT COUNT(e) FROM UserEvent e WHERE e.articleId = :articleId AND e.eventTime > :cutoffTime")
    Long countRecentEventsByArticle(@Param("articleId") UUID articleId, @Param("cutoffTime") LocalDateTime cutoffTime);

    // Find trending articles based on event count in a geographic area
    @Query(value = "SELECT article_id, COUNT(*) as event_count " +
            "FROM user_events " +
            "WHERE event_time > :cutoffTime " +
            "AND (6371 * acos(cos(radians(:latitude)) * cos(radians(latitude)) * cos(radians(longitude) - radians(:longitude)) + sin(radians(:latitude)) * sin(radians(latitude)))) < :radius " +
            "GROUP BY article_id " +
            "ORDER BY event_count DESC " +
            "LIMIT :limit", nativeQuery = true)
    List<Object[]> findTrendingArticleIds(
            @Param("cutoffTime") LocalDateTime cutoffTime,
            @Param("latitude") Double latitude,
            @Param("longitude") Double longitude,
            @Param("radius") Double radius,
            @Param("limit") int limit);
}
