package br.com.microservices.orchestrated.orderservice.core.service;

import br.com.microservices.orchestrated.orderservice.config.exception.ValidationException;
import br.com.microservices.orchestrated.orderservice.core.document.Event;
import br.com.microservices.orchestrated.orderservice.core.dto.EventFilters;
import br.com.microservices.orchestrated.orderservice.core.repository.EventRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class EventService {

  private final EventRepository eventRepository;

  public Event save(Event event) {
    return eventRepository.save(event);
  }

  public void notifyEnding(Event event) {
    event.setOrderId(event.getOrderId());
    event.setCreatedAt(LocalDateTime.now());
    save(event);

    log.info(
        "Order {} with saga notified! Transaction ID: {}",
        event.getOrderId(),
        event.getTransactionId());
  }

  public List<Event> findAll() {
    return eventRepository.findAllByOrderByCreatedAtDesc();
  }

  public Event findByFilters(EventFilters filters) {
    validateEmptyFilter(filters);

    if (ObjectUtils.isNotEmpty(filters.getOrderId())) {
      return findByOrderId(filters.getOrderId());
    }

    return findByTransactionId(filters.getTransactionId());
  }

  private void validateEmptyFilter(EventFilters eventFilters) {
    if (ObjectUtils.isEmpty(eventFilters.getOrderId())
        && ObjectUtils.isEmpty(eventFilters.getTransactionId())) {
      throw new ValidationException("OrderID or TransactionID must by informed");
    }
  }

  private Event findByOrderId(String orderId) {
    return eventRepository
        .findTop1ByOrderIdOrderByCreatedAtDesc(orderId)
        .orElseThrow(() -> new ValidationException("Event not found with the given order ID"));
  }

  private Event findByTransactionId(String orderId) {
    return eventRepository
        .findTop1ByTransactionIdOrderByCreatedAtDesc(orderId)
        .orElseThrow(
            () -> new ValidationException("Event not found with the given transaction ID"));
  }
}
