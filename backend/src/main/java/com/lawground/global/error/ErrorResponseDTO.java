package com.lawground.global.error;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;
import java.util.List;

public record ErrorResponseDTO(
        ErrorCode code,
        String message,
        String requestId,
        @JsonFormat(shape = JsonFormat.Shape.STRING) Instant timestamp,
        List<FieldErrorDTO> fieldErrors) {
    public record FieldErrorDTO(String field, String message) {}
}
