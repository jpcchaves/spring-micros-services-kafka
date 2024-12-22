package br.com.microservices.orchestrated.orderservice.core.controller;

import br.com.microservices.orchestrated.orderservice.core.document.Event;
import br.com.microservices.orchestrated.orderservice.core.dto.EventFilters;
import br.com.microservices.orchestrated.orderservice.core.service.EventService;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("/api/event")
public class EventController {

  private final EventService eventService;

  @GetMapping
  public ResponseEntity<Event> findByFilters(EventFilters eventFilters) {
    return ResponseEntity.ok(eventService.findByFilters(eventFilters));
  }

  @GetMapping("/all")
  public ResponseEntity<List<Event>> findAll() {
    return ResponseEntity.ok(eventService.findAll());
  }
}
