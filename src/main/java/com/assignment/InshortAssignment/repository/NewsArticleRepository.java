package com.assignment.InshortAssignment.repository;

import com.assignment.InshortAssignment.model.NewsArticle;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NewsArticleRepository extends JpaRepository<NewsArticle, UUID> {

    // Find by category (case-insensitive)
    @Query("SELECT n FROM NewsArticle n JOIN n.category c WHERE LOWER(c) = LOWER(:category) ORDER BY n.publicationDate DESC")
    List<NewsArticle> findByCategory(@Param("category") String category, Pageable pageable);

    // Find by source name (case-insensitive using JPA method naming)
    List<NewsArticle> findBySourceNameIgnoreCaseOrderByPublicationDateDesc(String sourceName, Pageable pageable);

    // Find by relevance score greater than or equal to a threshold
    List<NewsArticle> findByRelevanceScoreGreaterThanEqualOrderByRelevanceScoreDesc(Double threshold, Pageable pageable);

    // Search in title and description
    @Query("SELECT n FROM NewsArticle n WHERE LOWER(n.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(n.description) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY n.relevanceScore DESC")
    List<NewsArticle> searchByTitleOrDescription(@Param("query") String query, Pageable pageable);

    // Find nearby articles using Haversine formula (distance in kilometers)
    @Query(value = "SELECT *, " +
            "(6371 * acos(cos(radians(:latitude)) * cos(radians(latitude)) * cos(radians(longitude) - radians(:longitude)) + sin(radians(:latitude)) * sin(radians(latitude)))) AS distance " +
            "FROM news_articles " +
            "WHERE (6371 * acos(cos(radians(:latitude)) * cos(radians(latitude)) * cos(radians(longitude) - radians(:longitude)) + sin(radians(:latitude)) * sin(radians(latitude)))) < :radius " +
            "ORDER BY distance", nativeQuery = true)
    List<NewsArticle> findNearbyArticles(@Param("latitude") Double latitude, @Param("longitude") Double longitude, @Param("radius") Double radius, Pageable pageable);

    // Additional query for trending articles by location (will be used in conjunction with user events)
    @Query(value = "SELECT n.* FROM news_articles n " +
            "JOIN user_events e ON n.id = e.article_id " +
            "WHERE (6371 * acos(cos(radians(:latitude)) * cos(radians(n.latitude)) * cos(radians(n.longitude) - radians(:longitude)) + sin(radians(:latitude)) * sin(radians(n.latitude)))) < :radius " +
            "GROUP BY n.id " +
            "ORDER BY COUNT(e.id) DESC, MAX(e.event_time) DESC", nativeQuery = true)
    List<NewsArticle> findTrendingArticles(@Param("latitude") Double latitude, @Param("longitude") Double longitude, @Param("radius") Double radius, Pageable pageable);
}
