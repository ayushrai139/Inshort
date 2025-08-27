package com.assignment.InshortAssignment.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "news_articles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsArticle {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private String url;

    @Column(name = "publication_date", nullable = false)
    private LocalDateTime publicationDate;

    @Column(name = "source_name", nullable = false)
    private String sourceName;

    @ElementCollection
    @CollectionTable(name = "article_categories", joinColumns = @JoinColumn(name = "article_id"))
    @Column(name = "category")
    private List<String> category;

    @Column(name = "relevance_score", nullable = false)
    private Double relevanceScore;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "llm_summary", length = 2000)
    private String llmSummary;
}
