package com.github.mofuncode.semantic_notes.service;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
    import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

/**
 * Service for interacting with OpenAI API
 * Provides methods to list and retrieve OpenAI models
 */
@Slf4j
@Service
public class OpenAIService {

    private final String apiKey;
    private OpenAIClient openAIClient;

    /**
     * Constructor with API key injection from application.properties
     * @param apiKey OpenAI API key from ${openai.api.key} property
     */
    public OpenAIService(@Value("${openai.api.key}") String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * Initialize OpenAI client after bean construction
     * Using @PostConstruct ensures the client is ready before any service methods are called
     */
    @PostConstruct
    private void initializeClient() {
        try {
            this.openAIClient = OpenAIOkHttpClient.builder()
                    .apiKey(apiKey)
                    .build();
            log.info("OpenAI client initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize OpenAI client", e);
            throw new RuntimeException("Failed to initialize OpenAI client", e);
        }
    }

    /**
     * Lists all available OpenAI models
     * Returns the model list page which contains data about available models
     * @return Object containing model list data (use .data() to get the list)
     * @throws RuntimeException if the API call fails
     */
    public Object listModels() {
        try {
            log.debug("Fetching list of OpenAI models");
            var models = openAIClient.models().list();

            log.info("Successfully fetched OpenAI models");
            log.debug("Models response: {}", models);

            return models.data();
        } catch (Exception e) {
            log.error("Error fetching OpenAI models", e);
            throw new RuntimeException("Failed to fetch OpenAI models", e);
        }
    }

    /**
     * Gets details of a specific model by ID
     * @param modelId The model ID (e.g., "gpt-4", "gpt-3.5-turbo", "text-embedding-ada-002")
     * @return Model details object
     * @throws RuntimeException if the model is not found or API call fails
     */
    public Object getModel(String modelId) {
        try {
            log.debug("Fetching details for model: {}", modelId);
            var model = openAIClient.models().retrieve(modelId);
            log.info("Successfully retrieved model: {}", modelId);
            log.debug("Model details: {}", model);
            return model;
        } catch (Exception e) {
            log.error("Error fetching model: {}", modelId, e);
            throw new RuntimeException("Failed to fetch model: " + modelId, e);
        }
    }

    /**
     * Checks if a specific model exists and is available
     * @param modelId The model ID to check
     * @return true if the model exists, false otherwise
     */
    public boolean isModelAvailable(String modelId) {
        try {
            getModel(modelId);
            return true;
        } catch (Exception e) {
            log.warn("Model {} is not available", modelId);
            return false;
        }
    }

    /**
     * Gets the OpenAI client instance for advanced usage
     * Useful for making chat completions, embeddings, and other OpenAI API calls
     * @return The configured OpenAI client
     */
    public OpenAIClient getClient() {
        return openAIClient;
    }
}
