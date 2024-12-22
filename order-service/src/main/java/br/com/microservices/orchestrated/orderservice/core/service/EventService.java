package br.com.microservices.orchestrated.orderservice.core.service;

import br.com.microservices.orchestrated.orderservice.core.document.Event;
import br.com.microservices.orchestrated.orderservice.core.repository.EventRepository;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
}
