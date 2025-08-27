package com.assignment.InshortAssignment.service;

import com.assignment.InshortAssignment.model.LlmQueryAnalysis;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LlmService {

    private final RestTemplate restTemplate;
    private final String apiKey;

    @Value("${gemini.model}")
    private String model;

    @Autowired
    public LlmService(RestTemplate restTemplate, String geminiApiKey) {
        this.restTemplate = restTemplate;
        this.apiKey = geminiApiKey;
    }

    public LlmQueryAnalysis analyzeQuery(String query) {
        try {
            String prompt = "Analyze this news query and extract: \n" +
                    "1. Entities (people, organizations, locations, events)\n" +
                    "2. Intent (category, source, search, nearby, score)\n\n" +
                    "IMPORTANT GUIDELINES:\n" +
                    "- Always extract all entities mentioned in the query (people, companies, locations, etc.)\n" +
                    "- When locations are mentioned with words like 'near', 'close to', use 'nearby' intent\n" +
                    "- When news sources are mentioned (e.g., 'New York Times', 'BBC'), include them as entities\n" +
                    "- For queries about specific topics (technology, sports, etc.), use 'category' intent\n" +
                    "- A query can have multiple intents when appropriate\n" +
                    "- Never leave entities or intent empty\n\n" +
                    "Examples:\n" +
                    "Query: \"Latest developments in the Elon Musk Twitter acquisition near Palo Alto\"\n" +
                    "Entities: [\"Elon Musk\", \"Twitter\", \"Palo Alto\"]\n" +
                    "Intent: \"nearby\"\n\n" +
                    "Query: \"Top technology news from the New York Times\"\n" +
                    "Entities: [\"technology\", \"New York Times\"]\n" +
                    "Intent: [\"category\", \"source\"]\n\n" +
                    "Now analyze this query: \"" + query + "\"\n\n" +
                    "Return in this exact JSON format:\n" +
                    "{\"entities\": [\"entity1\", \"entity2\", ...], \"intent\": [\"intent1\", \"intent2\", ...]}\n";

            String response = callGeminiApi(prompt);

            // Extract JSON from response (Gemini may include extra text)
            String jsonResponse = extractJsonFromResponse(response);

            try {
                // Using org.json library for proper JSON parsing
                org.json.JSONObject jsonObj = new org.json.JSONObject(jsonResponse);

                // Get entities array
                org.json.JSONArray entitiesArray = jsonObj.getJSONArray("entities");
                List<String> entities = new ArrayList<>();
                for (int i = 0; i < entitiesArray.length(); i++) {
                    entities.add(entitiesArray.getString(i));
                }

                // Get intent
                org.json.JSONArray intentArray = jsonObj.getJSONArray("intent");
                List<String> intents = new ArrayList<>();
                for (int i = 0; i < intentArray.length(); i++) {
                    intents.add(intentArray.getString(i));
                }

                // Ensure entities is never empty
                if (entities.isEmpty()) {
                    // If no entities were extracted, use words from the query
                    entities = Arrays.stream(query.split("\\s+"))
                            .filter(word -> word.length() > 3) // Only use significant words
                            .limit(2) // Take up to 2 words
                            .toList();

                    // If still empty, add a generic entity
                    if (entities.isEmpty()) {
                        entities = List.of("news");
                    }
                }

                // Ensure intent is never empty
                if (intents.isEmpty()) {
                    intents.add("search"); // Default intent
                }

                return LlmQueryAnalysis.builder()
                        .entities(entities)
                        .intent(intents)
                        .originalQuery(query)
                        .build();
            } catch (Exception e) {
                System.err.println("Error parsing LLM response: " + e.getMessage());
                return performFallbackAnalysis(query);
            }
        } catch (Exception e) {
            // Fallback analysis if Gemini fails
            System.err.println("Error calling Gemini API: " + e.getMessage());
            return performFallbackAnalysis(query);
        }
    }

    public String generateSummary(String content) {
        try {
            String prompt = "Summarize this news article content in 2-3 sentences:\n\n" + content;

            return callGeminiApi(prompt);
        } catch (Exception e) {
            return "Summary not available.";
        }
    }

    private String callGeminiApi(String prompt) {
        // Correct endpoint for Gemini API (using gemini-2.0-flash model)
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-goog-api-key", apiKey); // Using the correct header for API key

        // Build request body according to Gemini API format
        Map<String, Object> requestBody = new HashMap<>();
        Map<String, Object> content = new HashMap<>();
        Map<String, Object> part = new HashMap<>();

        part.put("text", prompt);

        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(part);

        content.put("parts", parts);

        List<Map<String, Object>> contents = new ArrayList<>();
        contents.add(content);

        requestBody.put("contents", contents);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);

            // Extract text from Gemini response format
            if (response != null && response.containsKey("candidates")) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
                if (candidates != null && !candidates.isEmpty()) {
                    Map<String, Object> candidate = candidates.get(0);
                    Map<String, Object> candidateContent = (Map<String, Object>) candidate.get("content");
                    List<Map<String, Object>> responseParts = (List<Map<String, Object>>) candidateContent.get("parts");

                    return (String) responseParts.get(0).get("text");
                }
            }

            System.err.println("Unexpected response format from Gemini API");
            return "";
        } catch (Exception e) {
            // Log the error and return an empty string
            System.err.println("Error calling Gemini API: " + e.getMessage());
            return "";
        }
    }

    private String extractJsonFromResponse(String response) {
        // Look for JSON pattern in the response
        int jsonStart = response.indexOf("{");
        int jsonEnd = response.lastIndexOf("}") + 1;

        if (jsonStart >= 0 && jsonEnd > jsonStart) {
            return response.substring(jsonStart, jsonEnd);
        }

        // If no JSON found, return the original response
        return response;
    }

    private LlmQueryAnalysis performFallbackAnalysis(String query) {
        // Simple fallback logic if Gemini fails
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
                .intent(List.of(intent))
                .originalQuery(query)
                .build();
    }
}
