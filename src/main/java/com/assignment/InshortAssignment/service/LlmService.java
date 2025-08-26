package com.assignment.InshortAssignment.service;

import com.assignment.InshortAssignment.model.LlmQueryAnalysis;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class LlmService {

    private final OpenAiService openAiService;

    @Value("${openai.model}")
    private String model;

    @Autowired
    public LlmService(OpenAiService openAiService) {
        this.openAiService = openAiService;
    }

    public LlmQueryAnalysis analyzeQuery(String query) {
        try {
            String prompt = "Analyze this news query and extract: \n" +
                    "1. Entities (people, organizations, locations, events)\n" +
                    "2. Intent (category, source, search, nearby, score)\n\n" +
                    "Query: \"" + query + "\"\n\n" +
                    "Return in this exact JSON format:\n" +
                    "{\"entities\": [\"entity1\", \"entity2\", ...], \"intent\": \"primary_intent\"}";

            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new ChatMessage("system", "You are a helpful assistant that analyzes news queries."));
            messages.add(new ChatMessage("user", prompt));

            ChatCompletionRequest completionRequest = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(messages)
                    .build();

            String response = openAiService.createChatCompletion(completionRequest)
                    .getChoices().get(0).getMessage().getContent();

            // Simple parsing of the JSON response - in production, use a proper JSON parser
            String entitiesStr = response.substring(response.indexOf("\"entities\": [") + 13, response.indexOf("]"));
            String intent = response.substring(response.indexOf("\"intent\": \"") + 10, response.indexOf("\"", response.indexOf("\"intent\": \"") + 10));

            List<String> entities = Arrays.stream(entitiesStr.split(","))
                    .map(e -> e.replace("\"", "").trim())
                    .filter(e -> !e.isEmpty())
                    .toList();

            return LlmQueryAnalysis.builder()
                    .entities(entities)
                    .intent(intent)
                    .originalQuery(query)
                    .build();
        } catch (Exception e) {
            // Fallback analysis if OpenAI fails
            return performFallbackAnalysis(query);
        }
    }

    public String generateSummary(String content) {
        try {
            String prompt = "Summarize this news article content in 2-3 sentences:\n\n" + content;

            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new ChatMessage("system", "You are a helpful assistant that summarizes news articles."));
            messages.add(new ChatMessage("user", prompt));

            ChatCompletionRequest completionRequest = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(messages)
                    .build();

            return openAiService.createChatCompletion(completionRequest)
                    .getChoices().get(0).getMessage().getContent();
        } catch (Exception e) {
            return "Summary not available.";
        }
    }

    private LlmQueryAnalysis performFallbackAnalysis(String query) {
        // Simple fallback logic if OpenAI fails
        List<String> entities = new ArrayList<>();
        String intent = "search"; // default intent

        query = query.toLowerCase();

        // Simple intent detection
        if (query.contains("near") || query.contains("nearby") || query.contains("close to")) {
            intent = "nearby";
        } else if (query.contains("from") && (query.contains("times") || query.contains("post") || query.contains("journal"))) {
            intent = "source";
        } else if (query.contains("technology") || query.contains("business") || query.contains("sports") || query.contains("entertainment")) {
            intent = "category";
        } else if (query.contains("relevant") || query.contains("score") || query.contains("high quality")) {
            intent = "score";
        }

        // Simple entity extraction - just take nouns and proper nouns
        String[] words = query.split("\\s+");
        for (String word : words) {
            // Add capitalized words as potential entities
            if (!word.isEmpty() && Character.isUpperCase(word.charAt(0))) {
                entities.add(word.replaceAll("[^a-zA-Z]", ""));
            }
        }

        return LlmQueryAnalysis.builder()
                .entities(entities)
                .intent(intent)
                .originalQuery(query)
                .build();
    }
}
