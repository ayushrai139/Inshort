package com.assignment.InshortAssignment.service;

import com.assignment.InshortAssignment.model.NewsArticle;
import com.assignment.InshortAssignment.repository.NewsArticleRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class DataLoader implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataLoader.class);
    private final NewsArticleRepository newsArticleRepository;
    private final ResourceLoader resourceLoader;

    @Value("${news.data.directory:#{null}}")
    private String dataDirectory;

    @Autowired
    public DataLoader(NewsArticleRepository newsArticleRepository, ResourceLoader resourceLoader) {
        this.newsArticleRepository = newsArticleRepository;
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            // Check if we have any articles already
            long count = newsArticleRepository.count();
            if (count > 0) {
                logger.info("Database already contains {} articles. Skipping data load.", count);
                return;
            }

            // Try to load from directory if specified
            if (dataDirectory != null) {
                try {
                    loadNewsData();
                    return; // Return if successful
                } catch (Exception e) {
                    logger.error("Error loading news data from directory: {}", e.getMessage());
                }
            }

            // If we get here, either no directory was specified or loading from directory failed
            // Let's try to load the sample data
            loadSampleNewsData();
        } catch (Exception e) {
            logger.error("Error during data loading: {}", e.getMessage(), e);
        }
    }

    public void loadNewsData() throws IOException {
        long count = newsArticleRepository.count();
        if (count > 0) {
            logger.info("Database already contains {} articles. Skipping data load.", count);
            return;
        }

        Path directory = Paths.get(dataDirectory);
        if (!Files.exists(directory)) {
            logger.error("Data directory does not exist: {}", dataDirectory);
            return;
        }

        logger.info("Loading news data from {}", dataDirectory);

        try (Stream<Path> paths = Files.walk(directory)) {
            List<Path> jsonFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".json"))
                    .collect(Collectors.toList());

            if (jsonFiles.isEmpty()) {
                logger.warn("No JSON files found in directory: {}", dataDirectory);
                return;
            }

            logger.info("Found {} JSON files in directory", jsonFiles.size());
            ObjectMapper objectMapper = new ObjectMapper()
                    .registerModule(new JavaTimeModule());

            for (Path jsonFile : jsonFiles) {
                logger.info("Processing file: {}", jsonFile);

                try {
                    // Read the JSON file
                    String content = Files.readString(jsonFile);

                    // Parse JSON to list of maps
                    List<Map<String, Object>> articleMaps = objectMapper.readValue(
                            content, new TypeReference<List<Map<String, Object>>>() {});

                    // Process articles in batches
                    processArticlesInBatches(articleMaps);

                    logger.info("Successfully processed file: {}", jsonFile);
                } catch (Exception e) {
                    logger.error("Error processing file: " + jsonFile, e);
                }
            }
        }
    }

    private NewsArticle mapToNewsArticle(Map<String, Object> map) {
        NewsArticle article = new NewsArticle();

        // Set ID
        String idStr = (String) map.get("id");
        article.setId(UUID.fromString(idStr));

        // Set basic properties
        article.setTitle((String) map.get("title"));
        article.setDescription((String) map.get("description"));
        article.setUrl((String) map.get("url"));
        article.setSourceName((String) map.get("source_name"));

        // Set relevance score
        Object scoreObj = map.get("relevance_score");
        if (scoreObj instanceof Number) {
            article.setRelevanceScore(((Number) scoreObj).doubleValue());
        }

        // Set latitude and longitude
        Object latObj = map.get("latitude");
        Object lonObj = map.get("longitude");
        if (latObj instanceof Number && lonObj instanceof Number) {
            article.setLatitude(((Number) latObj).doubleValue());
            article.setLongitude(((Number) lonObj).doubleValue());
        }

        // Set publication date
        String dateStr = (String) map.get("publication_date");
        if (dateStr != null) {
            try {
                // Try with the format that includes seconds
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
                article.setPublicationDate(LocalDateTime.parse(dateStr, formatter));
            } catch (Exception e) {
                // If that fails, try an alternative format
                try {
                    logger.warn("Failed to parse date with primary format, trying alternative: {}", dateStr);
                    DateTimeFormatter alternativeFormatter = DateTimeFormatter.ISO_DATE_TIME;
                    article.setPublicationDate(LocalDateTime.parse(dateStr, alternativeFormatter));
                } catch (Exception ex) {
                    logger.error("Failed to parse publication date: {}", dateStr, ex);
                    // Set a default date to avoid null
                    article.setPublicationDate(LocalDateTime.now());
                }
            }
        } else {
            // Set a default date if none provided
            article.setPublicationDate(LocalDateTime.now());
        }

        // Set categories
        Object categoryObj = map.get("category");
        if (categoryObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> categories = (List<String>) categoryObj;
            article.setCategory(categories);
        } else {
            // Set a default category if none provided
            article.setCategory(List.of("General"));
        }

        return article;
    }

    // Method to load a single JSON file for testing
    public void loadSingleJsonFile(String filePath) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule());

        Path path = Paths.get(filePath);
        String content = Files.readString(path);

        List<Map<String, Object>> articleMaps = objectMapper.readValue(
                content, new TypeReference<List<Map<String, Object>>>() {});

        List<NewsArticle> articles = new ArrayList<>();
        for (Map<String, Object> articleMap : articleMaps) {
            NewsArticle article = mapToNewsArticle(articleMap);
            articles.add(article);
        }

        newsArticleRepository.saveAll(articles);
        logger.info("Loaded {} news articles from single file", articles.size());
    }

    // Load news data from the sample file in resources
    public void loadSampleNewsData() {
        try {
            Resource resource = resourceLoader.getResource("classpath:sample-news-data.json");
            if (!resource.exists()) {
                logger.error("Sample news data file not found in resources");
                return;
            }

            logger.info("Loading news from sample data file");

            ObjectMapper objectMapper = new ObjectMapper()
                    .registerModule(new JavaTimeModule());

            try (InputStream inputStream = resource.getInputStream()) {
                List<Map<String, Object>> articleMaps = objectMapper.readValue(
                        inputStream, new TypeReference<List<Map<String, Object>>>() {});

                processArticlesInBatches(articleMaps);
            }
        } catch (Exception e) {
            logger.error("Error loading sample news data: {}", e.getMessage(), e);
        }
    }

    private void processArticlesInBatches(List<Map<String, Object>> articleMaps) {
        final int BATCH_SIZE = 20;
        int totalArticles = articleMaps.size();
        logger.info("Processing {} articles in batches of {}", totalArticles, BATCH_SIZE);

        for (int i = 0; i < totalArticles; i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, totalArticles);
            List<Map<String, Object>> batch = articleMaps.subList(i, endIndex);

            List<NewsArticle> articles = new ArrayList<>();
            for (Map<String, Object> articleMap : batch) {
                try {
                    NewsArticle article = mapToNewsArticle(articleMap);
                    articles.add(article);
                } catch (Exception e) {
                    logger.error("Error mapping article: {}", e.getMessage());
                }
            }

            if (!articles.isEmpty()) {
                try {
                    newsArticleRepository.saveAll(articles);
                    logger.info("Saved batch of {} articles ({}-{})", articles.size(), i+1, endIndex);
                } catch (Exception e) {
                    logger.error("Error saving batch: {}", e.getMessage(), e);
                }
            }
        }

        logger.info("Completed loading articles");
    }
}
