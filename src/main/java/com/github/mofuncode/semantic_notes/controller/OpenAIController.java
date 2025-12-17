package com.github.mofuncode.semantic_notes.controller;

import com.github.mofuncode.semantic_notes.service.OpenAIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j // for logging
@RestController
@RequestMapping("/api/openai")
@RequiredArgsConstructor // Lombok generates constructor for final field
public class OpenAIController {
    private final OpenAIService openAIService;

    /**
     * Lists all available OpenAI models
     * GET /api/openai/models
     */
    @GetMapping("/models")
    public ResponseEntity<Object> listModels() {
        log.info("Fetching list of OpenAI models");
        Object models = openAIService.listModels();
        return ResponseEntity.ok(models);
    }

}
