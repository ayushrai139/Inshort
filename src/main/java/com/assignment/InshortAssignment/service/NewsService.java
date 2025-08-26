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
        return newsArticleRepository.findBySourceNameOrderByPublicationDateDesc(source, pageable);
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

        // Based on the intent, retrieve appropriate articles
        List<NewsArticle> articles;

        switch (analysis.getIntent().toLowerCase()) {
            case "category":
                // Extract category from entities
                if (!analysis.getEntities().isEmpty()) {
                    String category = analysis.getEntities().get(0);
                    articles = getArticlesByCategory(category, limit);
                } else {
                    articles = Collections.emptyList();
                }
                break;

            case "source":
                // Extract source from entities
                if (!analysis.getEntities().isEmpty()) {
                    String source = analysis.getEntities().get(0);
                    articles = getArticlesBySource(source, limit);
                } else {
                    articles = Collections.emptyList();
                }
                break;

            case "score":
                // Use a default threshold or extract from query
                articles = getArticlesByRelevanceScore(0.7, limit);
                break;

            case "nearby":
                // Use user location for nearby articles
                if (location != null) {
                    articles = getNearbyArticles(location, limit);
                } else {
                    articles = Collections.emptyList();
                }
                break;

            case "search":
            default:
                // Use the original query or extracted entities for search
                articles = searchArticles(query, limit);
                break;
        }

        // Enrich articles with LLM-generated summaries if not already present
        return articles.stream()
                .map(this::enrichArticleWithSummary)
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
