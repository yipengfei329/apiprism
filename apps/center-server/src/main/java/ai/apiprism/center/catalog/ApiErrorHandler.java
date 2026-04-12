package ai.apiprism.center.catalog;

import ai.apiprism.center.exceptions.RegistrationNotFoundException;
import ai.apiprism.openapi.exceptions.NormalizationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiErrorHandler {

    @ExceptionHandler(RegistrationNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleNotFound(RegistrationNotFoundException exception) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        detail.setTitle("Registration not found");
        detail.setDetail(exception.getMessage());
        return detail;
    }

    @ExceptionHandler(NormalizationException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ProblemDetail handleNormalizationFailure(NormalizationException exception) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        detail.setTitle("OpenAPI spec could not be processed");
        detail.setDetail(exception.getMessage());
        return detail;
    }
}

