package br.com.microservices.orchestrated.paymentservice.core.service;

import br.com.microservices.orchestrated.paymentservice.config.exception.ValidationException;
import br.com.microservices.orchestrated.paymentservice.core.dto.Event;
import br.com.microservices.orchestrated.paymentservice.core.dto.History;
import br.com.microservices.orchestrated.paymentservice.core.dto.OrderProducts;
import br.com.microservices.orchestrated.paymentservice.core.enums.EPaymentStatus;
import br.com.microservices.orchestrated.paymentservice.core.enums.ESagaStatus;
import br.com.microservices.orchestrated.paymentservice.core.model.Payment;
import br.com.microservices.orchestrated.paymentservice.core.producer.KafkaProducer;
import br.com.microservices.orchestrated.paymentservice.core.repository.PaymentRepository;
import br.com.microservices.orchestrated.paymentservice.core.utils.JsonUtil;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class PaymentService {
  private static final String CURRENT_SOURCE = "PAYMENT_SERVICE";
  private static final Double REDUCE_SUM_VALUE = 0.0;
  private static final Double MIN_AMOUNT_VALUE = 0.1;

  private final JsonUtil jsonUtil;
  private final KafkaProducer producer;
  private final PaymentRepository paymentRepository;

  public void realizePayment(Event event) {
    try {
      checkCurrentValidation(event);
      creatingPendingPayment(event);
      Payment payment = findByOrderIdAndTransactionId(event);

      validateAmount(payment.getTotalAmount());

      changePaymentToSuccess(payment);

      handleSuccess(event);
    } catch (Exception e) {
      log.error("Error processing payment: ", e);
      handleFailCurrentNotExecuted(event, e.getMessage());
    }

    producer.sendEvent(jsonUtil.toJson(event));
  }

  public void realizeRefund(Event event) {
    event.setStatus(ESagaStatus.FAIL);
    event.setSource(CURRENT_SOURCE);

    try {
      changePaymentStatusToRefund(event);
      addHistory(event, "Rollback executed for payment!");
    } catch (Exception e) {
      addHistory(event, "Rollback not executed for payment! Exception: " + e.getMessage());
    }

    producer.sendEvent(jsonUtil.toJson(event));
  }

  private void changePaymentStatusToRefund(Event event) {
    Payment payment = findByOrderIdAndTransactionId(event);
    payment.setStatus(EPaymentStatus.REFUND);
    setEventAmountItems(event, payment);
    savePayment(payment);
  }

  private void checkCurrentValidation(Event event) {
    if (paymentRepository.existsByOrderIdAndTransactionId(
        event.getPayload().getId(), event.getTransactionId())) {
      throw new ValidationException(
          "There's another transaction for this transactionId: " + event.getTransactionId());
    }
  }

  private void creatingPendingPayment(Event event) {
    Double totalAmount = calculateAmount(event);
    Integer totalItems = calculateTotalItems(event);

    Payment payment =
        Payment.builder()
            .orderId(event.getPayload().getId())
            .transactionId(event.getTransactionId())
            .totalAmount(totalAmount)
            .totalItems(totalItems)
            .build();

    savePayment(payment);

    setEventAmountItems(event, payment);
  }

  private Double calculateAmount(Event event) {
    return event.getPayload().getProducts().stream()
        .map(product -> product.getQuantity() * product.getProduct().getUnitValue())
        .reduce(REDUCE_SUM_VALUE, Double::sum);
  }

  private Integer calculateTotalItems(Event event) {
    return event.getPayload().getProducts().stream()
        .map(OrderProducts::getQuantity)
        .reduce(REDUCE_SUM_VALUE.intValue(), Integer::sum);
  }

  private void setEventAmountItems(Event event, Payment payment) {
    event.getPayload().setTotalAmount(payment.getTotalAmount());
    event.getPayload().setTotalItems(payment.getTotalItems());
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

  private void handleSuccess(Event event) {
    event.setStatus(ESagaStatus.SUCCESS);
    event.setSource(CURRENT_SOURCE);

    addHistory(event, "Payment successful!");
  }

  private void handleFailCurrentNotExecuted(Event event, String message) {
    event.setStatus(ESagaStatus.ROLLBACK_PENDING);
    event.setSource(CURRENT_SOURCE);
    addHistory(event, "Products validation failed! Exception: ".concat(message));
  }

  private void changePaymentToSuccess(Payment payment) {
    payment.setStatus(EPaymentStatus.SUCCESS);
    savePayment(payment);
  }

  private void validateAmount(Double amount) {
    if (amount < MIN_AMOUNT_VALUE) {
      throw new ValidationException("The minimum amount available is R$ " + MIN_AMOUNT_VALUE);
    }
  }

  private Payment findByOrderIdAndTransactionId(Event event) {
    return paymentRepository
        .findByOrderIdAndTransactionId(event.getPayload().getId(), event.getTransactionId())
        .orElseThrow(
            () ->
                new ValidationException(
                    "Payment not found with the given orderId and transactionId"));
  }

  private void savePayment(Payment payment) {
    paymentRepository.save(payment);
  }
}
