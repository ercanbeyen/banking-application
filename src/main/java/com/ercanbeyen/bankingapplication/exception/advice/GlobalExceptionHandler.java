package com.ercanbeyen.bankingapplication.exception.advice;

import com.ercanbeyen.bankingapplication.exception.BadRequestException;
import com.ercanbeyen.bankingapplication.exception.ResourceConflictException;
import com.ercanbeyen.bankingapplication.exception.ResourceExpectationFailedException;
import com.ercanbeyen.bankingapplication.exception.ResourceNotFoundException;
import com.ercanbeyen.bankingapplication.dto.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleMethodArgumentValidationExceptions(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new HashMap<>();

        exception.getBindingResult()
                .getAllErrors()
                .forEach(error -> {
                    String field = ((FieldError) error).getField();
                    String message = error.getDefaultMessage();
                    errors.put(field, message);
                });

        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MissingPathVariableException.class, MissingRequestHeaderException.class})
    public ResponseEntity<ErrorResponse> handleMissingRequestExceptions(Exception exception) {
        String message = exception.getMessage();
        int beginIndex = message.indexOf("'") + 1;
        String remainingMessage = message.substring(beginIndex);
        int endIndex = beginIndex + remainingMessage.indexOf("'");
        String missingRequestParameter = message.substring(beginIndex, endIndex);

        String[] words = missingRequestParameter.split("-");
        StringBuilder stringBuilder = new StringBuilder();

        for (String word : words) {
            stringBuilder.append(word);
            stringBuilder.append(" ");
        }

        stringBuilder.append("is missing");

        Exception modifiedException = new Exception(stringBuilder.toString());
        return generateErrorResponse(modifiedException, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Map<String, String>> handleMethodValidationException(HandlerMethodValidationException exception) {
        Map<String, String> errors = new HashMap<>();

        exception.getParameterValidationResults()
                .forEach(parameterValidationResult -> {
                    String parameter = parameterValidationResult.getMethodParameter().getParameter().getName();
                    for (MessageSourceResolvable messageSourceResolvable : parameterValidationResult.getResolvableErrors()) {
                        String message = messageSourceResolvable.getDefaultMessage();
                        errors.put(parameter, message);
                    }
                });

        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({BadRequestException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ErrorResponse> handleBadRequestExceptions(Exception exception) {
        return generateErrorResponse(exception, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ErrorResponse> handleLockedException(Exception exception) {
        return generateErrorResponse(exception, HttpStatus.LOCKED);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(Exception exception) {
        return generateErrorResponse(exception, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(Exception exception) {
        return generateErrorResponse(exception, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(Exception exception) {
        return generateErrorResponse(exception, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<ErrorResponse> handleResourceConflictException(Exception exception) {
        return generateErrorResponse(exception, HttpStatus.CONFLICT);
    }

    @ExceptionHandler({MaxUploadSizeExceededException.class, ResourceExpectationFailedException.class})
    public ResponseEntity<ErrorResponse> handleResourceFailedExceptions(Exception exception) {
        return generateErrorResponse(exception, HttpStatus.EXPECTATION_FAILED);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralExceptions(Exception exception) {
        log.error("Internal Server Error occurred. Message: {}", exception.getMessage());
        Exception modifiedException = new Exception("While operation is processing, error was occurred in the server");
        return generateErrorResponse(modifiedException, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ErrorResponse> generateErrorResponse(Exception exception, HttpStatus httpStatus) {
        ErrorResponse response = new ErrorResponse(httpStatus.value(), exception.getMessage(), Instant.now());
        return new ResponseEntity<>(response, httpStatus);
    }
}
