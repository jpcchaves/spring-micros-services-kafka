package br.com.microservices.orchestrated.inventoryservice.config.exception;

import br.com.microservices.orchestrated.orderservice.config.exception.ExceptionDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ExceptionGlobalHandler {

  @ExceptionHandler(ValidationException.class)
  public ResponseEntity<?> handleValidationException(ValidationException validationException) {

    ExceptionDetails exceptionDetails =
        new ExceptionDetails(validationException.getMessage(), HttpStatus.BAD_REQUEST.value());

    return ResponseEntity.badRequest().body(exceptionDetails);
  }
}
