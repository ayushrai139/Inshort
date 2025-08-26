package com.assignment.InshortAssignment.controller;

import com.assignment.InshortAssignment.model.NewsArticle;
import com.assignment.InshortAssignment.model.UserLocation;
import com.assignment.InshortAssignment.service.NewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/news")
public class NewsController {

    private final NewsService newsService;

    @Autowired
    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    @GetMapping("/query")
    public ResponseEntity<Map<String, Object>> queryNews(
            @RequestParam String query,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon,
            @RequestParam(required = false, defaultValue = "10.0") Double radius,
            @RequestParam(required = false, defaultValue = "5") Integer limit) {

        UserLocation location = null;
        if (lat != null && lon != null) {
            location = UserLocation.builder()
                    .latitude(lat)
                    .longitude(lon)
                    .radius(radius)
                    .build();
        }

        List<NewsArticle> articles = newsService.processQuery(query, location, limit);

        Map<String, Object> response = new HashMap<>();
        response.put("articles", articles);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<Map<String, Object>> getByCategory(
            @PathVariable String category,
            @RequestParam(required = false, defaultValue = "5") Integer limit) {

        List<NewsArticle> articles = newsService.getArticlesByCategory(category, limit);

        Map<String, Object> response = new HashMap<>();
        response.put("articles", articles);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/source/{source}")
    public ResponseEntity<Map<String, Object>> getBySource(
            @PathVariable String source,
            @RequestParam(required = false, defaultValue = "5") Integer limit) {

        List<NewsArticle> articles = newsService.getArticlesBySource(source, limit);

        Map<String, Object> response = new HashMap<>();
        response.put("articles", articles);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/score")
    public ResponseEntity<Map<String, Object>> getByScore(
            @RequestParam(defaultValue = "0.7") Double threshold,
            @RequestParam(required = false, defaultValue = "5") Integer limit) {

        List<NewsArticle> articles = newsService.getArticlesByRelevanceScore(threshold, limit);

        Map<String, Object> response = new HashMap<>();
        response.put("articles", articles);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> search(
            @RequestParam String query,
            @RequestParam(required = false, defaultValue = "5") Integer limit) {

        List<NewsArticle> articles = newsService.searchArticles(query, limit);

        Map<String, Object> response = new HashMap<>();
        response.put("articles", articles);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/nearby")
    public ResponseEntity<Map<String, Object>> getNearby(
            @RequestParam Double lat,
            @RequestParam Double lon,
            @RequestParam(required = false, defaultValue = "10.0") Double radius,
            @RequestParam(required = false, defaultValue = "5") Integer limit) {

        UserLocation location = UserLocation.builder()
                .latitude(lat)
                .longitude(lon)
                .radius(radius)
                .build();

        List<NewsArticle> articles = newsService.getNearbyArticles(location, limit);

        Map<String, Object> response = new HashMap<>();
        response.put("articles", articles);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/trending")
    public ResponseEntity<Map<String, Object>> getTrending(
            @RequestParam Double lat,
            @RequestParam Double lon,
            @RequestParam(required = false, defaultValue = "10.0") Double radius,
            @RequestParam(required = false, defaultValue = "5") Integer limit) {

        UserLocation location = UserLocation.builder()
                .latitude(lat)
                .longitude(lon)
                .radius(radius)
                .build();

        List<NewsArticle> articles = newsService.getTrendingArticles(location, limit);

        Map<String, Object> response = new HashMap<>();
        response.put("articles", articles);

        return ResponseEntity.ok(response);
    }
}
