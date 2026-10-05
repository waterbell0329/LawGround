package com.lawground.question.controller;

import com.lawground.global.error.ApiException;
import com.lawground.global.error.ErrorCode;
import com.lawground.question.dto.CreateQuestionRequest;
import com.lawground.question.dto.QuestionResponse;
import com.lawground.question.service.QuestionService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/questions")
public class QuestionController {
    private final QuestionService questions;

    public QuestionController(QuestionService questions) {
        this.questions = questions;
    }

    @PostMapping(consumes = "application/json")
    public ResponseEntity<QuestionResponse> create(
            @Valid @RequestBody CreateQuestionRequest request) {
        var question = questions.create(request);
        return ResponseEntity.created(URI.create("/api/v1/questions/" + question.id()))
                .eTag(Long.toString(question.revision()))
                .body(question);
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuestionResponse> get(@PathVariable String id) {
        UUID parsed;
        try {
            parsed = UUID.fromString(id);
            if (!parsed.toString().equalsIgnoreCase(id)) throw new IllegalArgumentException();
        } catch (IllegalArgumentException invalid) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "문제 ID 형식을 확인해 주세요.");
        }
        var question = questions.get(parsed);
        return ResponseEntity.ok().eTag(Long.toString(question.revision())).body(question);
    }
}
