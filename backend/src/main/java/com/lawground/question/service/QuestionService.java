package com.lawground.question.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.lawground.auth.MemberResolver;
import com.lawground.global.error.ApiException;
import com.lawground.global.error.ErrorCode;
import com.lawground.global.error.ErrorResponseDTO;
import com.lawground.question.dto.CreateQuestionRequest;
import com.lawground.question.dto.QuestionResponse;
import com.lawground.question.entity.Question;
import com.lawground.question.repository.QuestionRepository;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuestionService {
    private final QuestionRepository questions;
    private final MemberResolver memberResolver;
    private final Clock clock;

    public QuestionService(
            QuestionRepository questions, MemberResolver memberResolver, Clock clock) {
        this.questions = questions;
        this.memberResolver = memberResolver;
        this.clock = clock;
    }

    @Transactional
    public QuestionResponse create(CreateQuestionRequest request) {
        UUID owner = memberResolver.requiredMemberId();
        if (request.choices().stream().map(CreateQuestionRequest.Choice::number).distinct().count()
                != 5) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCode.VALIDATION_FAILED,
                    "입력값을 확인해 주세요.",
                    List.of(
                            new ErrorResponseDTO.FieldErrorDTO(
                                    "choices", "선지 번호 1~5를 각각 한 번 입력하세요.")));
        }
        String hash = contentHash(request);
        if (questions.existsByOwnerIdAndContentHashAndDeletedAtIsNull(owner, hash))
            throw duplicate();
        try {
            return QuestionResponse.from(
                    questions.saveAndFlush(
                            new Question(
                                    UUID.randomUUID(),
                                    owner,
                                    request,
                                    hash,
                                    Instant.now(clock).truncatedTo(ChronoUnit.MICROS))));
        } catch (DataIntegrityViolationException conflict) {
            for (Throwable cause = conflict; cause != null; cause = cause.getCause()) {
                if (cause instanceof ConstraintViolationException violation
                        && "questions_active_owner_hash_uq".equals(violation.getConstraintName()))
                    throw duplicate();
            }
            throw conflict;
        }
    }

    @Transactional(readOnly = true)
    public QuestionResponse get(UUID id) {
        return QuestionResponse.from(
                questions
                        .findReadable(id, memberResolver.optionalMemberId())
                        .orElseThrow(
                                () ->
                                        new ApiException(
                                                HttpStatus.NOT_FOUND,
                                                ErrorCode.RESOURCE_NOT_FOUND,
                                                "문제를 찾을 수 없습니다.")));
    }

    private static ApiException duplicate() {
        return new ApiException(HttpStatus.CONFLICT, ErrorCode.DUPLICATE_QUESTION, "이미 등록한 문제입니다.");
    }

    static String contentHash(CreateQuestionRequest request) {
        var canonical = new LinkedHashMap<String, Object>();
        canonical.put("subject", request.subject().name());
        canonical.put("stem", request.stem());
        canonical.put("questionType", request.questionType().name());
        canonical.put(
                "choices",
                request.choices().stream()
                        .sorted(Comparator.comparing(CreateQuestionRequest.Choice::number))
                        .map(
                                choice -> {
                                    var item = new LinkedHashMap<String, Object>();
                                    item.put("number", choice.number());
                                    item.put("text", choice.text());
                                    return item;
                                })
                        .toList());
        canonical.put("providedAnswerNumber", request.providedAnswerNumber());
        canonical.put("referenceDate", request.referenceDate().toString());
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(
                                            JsonMapper.builder()
                                                    .build()
                                                    .writeValueAsBytes(canonical)));
        } catch (JsonProcessingException | NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("Canonical question hashing failed", impossible);
        }
    }
}
