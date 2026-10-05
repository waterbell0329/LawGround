package com.lawground.global.error;

import java.util.List;
import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final ErrorCode code;
    private final List<ErrorResponseDTO.FieldErrorDTO> fields;

    public ApiException(HttpStatus status, ErrorCode code, String message) {
        this(status, code, message, List.of());
    }

    public ApiException(
            HttpStatus status,
            ErrorCode code,
            String message,
            List<ErrorResponseDTO.FieldErrorDTO> fields) {
        super(message);
        this.status = status;
        this.code = code;
        this.fields = List.copyOf(fields);
    }

    public HttpStatus status() {
        return status;
    }

    public ErrorCode code() {
        return code;
    }

    public List<ErrorResponseDTO.FieldErrorDTO> fields() {
        return fields;
    }
}
