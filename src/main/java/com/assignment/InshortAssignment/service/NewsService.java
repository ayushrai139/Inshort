package com.assignment.InshortAssignment.service;

import com.assignment.InshortAssignment.model.LlmQueryAnalysis;
import com.assignment.InshortAssignment.model.NewsArticle;
import com.assignment.InshortAssignment.model.UserLocation;
import com.assignment.InshortAssignment.repository.NewsArticleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NewsService {

    private final NewsArticleRepository newsArticleRepository;
    private final LlmService llmService;

    @Autowired
    public NewsService(NewsArticleRepository newsArticleRepository, LlmService llmService) {
        this.newsArticleRepository = newsArticleRepository;
        this.llmService = llmService;
    }

    public List<NewsArticle> getArticlesByCategory(String category, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return newsArticleRepository.findByCategory(category, pageable);
    }

    public List<NewsArticle> getArticlesBySource(String source, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return newsArticleRepository.findBySourceNameIgnoreCaseOrderByPublicationDateDesc(source, pageable);
    }

    public List<NewsArticle> getArticlesByRelevanceScore(double threshold, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return newsArticleRepository.findByRelevanceScoreGreaterThanEqualOrderByRelevanceScoreDesc(threshold, pageable);
    }

    public List<NewsArticle> searchArticles(String query, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return newsArticleRepository.searchByTitleOrDescription(query, pageable);
    }

    public List<NewsArticle> getNearbyArticles(UserLocation location, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return newsArticleRepository.findNearbyArticles(
                location.getLatitude(),
                location.getLongitude(),
                location.getRadius(),
                pageable
        );
    }

    @Cacheable(value = "trendingArticles", key = "#location.latitude + '-' + #location.longitude + '-' + #location.radius")
    public List<NewsArticle> getTrendingArticles(UserLocation location, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return newsArticleRepository.findTrendingArticles(
                location.getLatitude(),
                location.getLongitude(),
                location.getRadius(),
                pageable
        );
    }

    public List<NewsArticle> processQuery(String query, UserLocation location, int limit) {
        // Analyze the query using LLM
        LlmQueryAnalysis analysis = llmService.analyzeQuery(query);

        // Based on the intents, retrieve appropriate articles
        List<NewsArticle> articles = new ArrayList<>();

        // Process all intents detected in the query
        for (String intent : analysis.getIntent()) {
            List<NewsArticle> intentArticles = new ArrayList<>();

            switch (intent.toLowerCase()) {
                case "category":
                    // Process all category entities
                    if (!analysis.getEntities().isEmpty()) {
                        for (String category : analysis.getEntities()) {
                            List<NewsArticle> categoryArticles = getArticlesByCategory(category, limit);
                            // Filter to ensure articles contain at least one entity in title or description
                            categoryArticles = filterArticlesByEntities(categoryArticles, analysis.getEntities());
                            intentArticles.addAll(categoryArticles);
                        }
                    }
                    break;

                case "source":
                    // Process all source entities
                    if (!analysis.getEntities().isEmpty()) {
                        for (String source : analysis.getEntities()) {
                            List<NewsArticle> sourceArticles = getArticlesBySource(source, limit);
                            // Filter to ensure articles contain at least one entity in title or description
                            sourceArticles = filterArticlesByEntities(sourceArticles, analysis.getEntities());
                            intentArticles.addAll(sourceArticles);
                        }
                    }
                    break;

                case "score":
                    // Use a default threshold or extract from query
                    List<NewsArticle> scoreArticles = getArticlesByRelevanceScore(0.7, limit);
                    // Filter to ensure articles contain at least one entity in title or description
                    scoreArticles = filterArticlesByEntities(scoreArticles, analysis.getEntities());
                    intentArticles.addAll(scoreArticles);
                    break;

                case "nearby":
                    // Use user location for nearby articles
                    if (location != null) {
                        List<NewsArticle> nearbyArticles = getNearbyArticles(location, limit);
                        // Filter to ensure articles contain at least one entity in title or description
                        nearbyArticles = filterArticlesByEntities(nearbyArticles, analysis.getEntities());
                        intentArticles.addAll(nearbyArticles);
                    }
                    break;

                case "search":
                    // Use the original query or extracted entities for search
                    intentArticles = searchArticles(query, limit);
                    if (!analysis.getEntities().isEmpty() && intentArticles.isEmpty()) {
                        // If direct search yields no results, try searching by entities
                        for (String entity : analysis.getEntities()) {
                            List<NewsArticle> entityArticles = searchArticles(entity, limit);
                            intentArticles.addAll(entityArticles);
                        }
                    }
                    break;

                default:
                    // For any unrecognized intent, fall back to text search
                    intentArticles = searchArticles(query, limit);
                    break;
            }

            // Add articles found for this intent to the overall result
            articles.addAll(intentArticles);
        }

        // If no articles were found and we haven't tried a general search, do that as a fallback
        if (articles.isEmpty() && !analysis.getIntent().contains("search")) {
            articles = searchArticles(query, limit);
        }

        // Remove duplicates if any (since we might add articles from multiple intents/sources/categories)
        List<NewsArticle> uniqueArticles = articles.stream()
                .distinct()
                .limit(limit) // Ensure we don't exceed the requested limit
                .collect(Collectors.toList());

        // Enrich articles with LLM-generated summaries if not already present
        return uniqueArticles.stream()
                .map(this::enrichArticleWithSummary)
                .collect(Collectors.toList());
    }

    /**
     * Filters a list of articles to only include those that contain at least one of the specified entities
     * in their title or description.
     *
     * @param articles The list of articles to filter
     * @param entities The entities to check for in the articles
     * @return A filtered list of articles that contain at least one entity
     */
    private List<NewsArticle> filterArticlesByEntities(List<NewsArticle> articles, List<String> entities) {
        // If no entities to filter by or no articles, return the original list
        if (entities == null || entities.isEmpty() || articles == null || articles.isEmpty()) {
            return articles;
        }

        return articles.stream()
                .filter(article -> {
                    String titleAndDescription = (article.getTitle() + " " + article.getDescription()).toLowerCase();

                    // Check if any entity is contained in the title or description
                    for (String entity : entities) {
                        if (titleAndDescription.contains(entity.toLowerCase())) {
                            return true;
                        }
                    }
                    return false;
                })
                .collect(Collectors.toList());
    }

    private NewsArticle enrichArticleWithSummary(NewsArticle article) {
        // Only generate summary if not already present
        if (article.getLlmSummary() == null || article.getLlmSummary().isEmpty()) {
            String content = article.getDescription();
            String summary = llmService.generateSummary(content);
            article.setLlmSummary(summary);

            // Save the updated article with summary
            newsArticleRepository.save(article);
        }

        return article;
    }
}
