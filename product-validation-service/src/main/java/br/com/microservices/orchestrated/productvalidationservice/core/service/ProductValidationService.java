package br.com.microservices.orchestrated.productvalidationservice.core.service;

import br.com.microservices.orchestrated.productvalidationservice.config.exception.ValidationException;
import br.com.microservices.orchestrated.productvalidationservice.core.dto.Event;
import br.com.microservices.orchestrated.productvalidationservice.core.dto.History;
import br.com.microservices.orchestrated.productvalidationservice.core.dto.OrderProducts;
import br.com.microservices.orchestrated.productvalidationservice.core.enums.ESagaStatus;
import br.com.microservices.orchestrated.productvalidationservice.core.model.Validation;
import br.com.microservices.orchestrated.productvalidationservice.core.producer.KafkaProducer;
import br.com.microservices.orchestrated.productvalidationservice.core.repository.ProductRepository;
import br.com.microservices.orchestrated.productvalidationservice.core.repository.ValidationRepository;
import br.com.microservices.orchestrated.productvalidationservice.core.utils.JsonUtil;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

@Service
@Slf4j
@AllArgsConstructor
public class ProductValidationService {

  private static final String CURRENT_SOURCE = "PRODUCT_VALIDATION_SERVICE";

  private final ProductRepository productRepository;
  private final ValidationRepository validationRepository;
  private final JsonUtil jsonUtil;
  private final KafkaProducer producer;

  public void validateExistingProducts(Event event) {
    try {
      checkCurrentValidation(event);
      createValidation(event, Boolean.TRUE);
      handleSuccess(event);
    } catch (Exception e) {
      log.error("Error trying to validate products: {0}", e);
      handleFailCurrentNotExecuted(event, e.getMessage());
    }

    producer.sendEvent(jsonUtil.toJson(event));
  }

  private void checkCurrentValidation(Event event) {
    validateProducts(event);

    // IDEMPOTENCIA
    if (validationRepository.existsByOrderIdAndTransactionId(
        event.getOrderId(), event.getTransactionId())) {
      throw new ValidationException("There's another transaction ID for this validation.");
    }

    event
        .getPayload()
        .getProducts()
        .forEach(
            product -> {
              validateProductInformed(product);
              validateProductExists(product);
            });
  }

  private void validateProductInformed(OrderProducts orderProducts) {
    if (ObjectUtils.isEmpty(orderProducts.getProduct())
        || ObjectUtils.isEmpty(orderProducts.getProduct().getCode())) {
      throw new ValidationException("Product must be informed!");
    }
  }

  private void validateProductExists(OrderProducts orderProducts) {
    if (!productRepository.existsByCode(orderProducts.getProduct().getCode())) {
      throw new ValidationException("Product doesn't exists in database!");
    }
  }

  private static void validateProducts(Event event) {
    if (ObjectUtils.isEmpty(event.getPayload())
        || CollectionUtils.isEmpty(event.getPayload().getProducts())) {
      throw new ValidationException("Product list is empty!");
    }

    if (ObjectUtils.isEmpty(event.getPayload().getId())
        || ObjectUtils.isEmpty(event.getPayload().getTransactionId())) {
      throw new ValidationException("OrderID and TransactionID must be informed!");
    }
  }

  private void createValidation(Event event, boolean success) {
    Validation validation =
        Validation.builder()
            .orderId(event.getPayload().getId())
            .transactionId(event.getTransactionId())
            .success(success)
            .build();

    validationRepository.save(validation);
  }

  private void handleSuccess(Event event) {
    event.setStatus(ESagaStatus.SUCCESS);
    event.setSource(CURRENT_SOURCE);
    addHistory(event, "Products validated successfully!");
  }

  private void addHistory(Event event, String message) {
    History history =
        History.builder()
            .source(event.getSource())
            .status(event.getStatus())
            .message(message)
            .createdAt(LocalDateTime.now())
            .build();

    event.addToHistory(history);
  }

  private void handleFailCurrentNotExecuted(Event event, String message) {
    event.setStatus(ESagaStatus.ROLLBACK_PENDING);
    event.setSource(CURRENT_SOURCE);
    addHistory(event, "Products validation failed! Exception: ".concat(message));
  }

  public void rollbackEvent(Event event) {
    failValidation(event);
    event.setStatus(ESagaStatus.FAIL);
    event.setSource(CURRENT_SOURCE);
    addHistory(event, "Rollback executed on product validation!");

    producer.sendEvent(jsonUtil.toJson(event));
  }

  private void failValidation(Event event) {
    validationRepository
        .findByOrderIdAndTransactionId(event.getPayload().getId(), event.getTransactionId())
        .ifPresentOrElse(
            validation -> {
              validation.setSuccess(false);
              validationRepository.save(validation);
            },
            () -> createValidation(event, false));
  }
}
