package br.com.microservices.orchestrated.inventoryservice.core.service;

import br.com.microservices.orchestrated.inventoryservice.config.exception.ValidationException;
import br.com.microservices.orchestrated.inventoryservice.core.dto.Event;
import br.com.microservices.orchestrated.inventoryservice.core.dto.History;
import br.com.microservices.orchestrated.inventoryservice.core.dto.Order;
import br.com.microservices.orchestrated.inventoryservice.core.dto.OrderProducts;
import br.com.microservices.orchestrated.inventoryservice.core.enums.ESagaStatus;
import br.com.microservices.orchestrated.inventoryservice.core.model.Inventory;
import br.com.microservices.orchestrated.inventoryservice.core.model.OrderInventory;
import br.com.microservices.orchestrated.inventoryservice.core.producer.KafkaProducer;
import br.com.microservices.orchestrated.inventoryservice.core.repository.InventoryRepository;
import br.com.microservices.orchestrated.inventoryservice.core.repository.OrderInventoryRepository;
import br.com.microservices.orchestrated.inventoryservice.core.utils.JsonUtil;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class InventoryService {

  private static final String CURRENT_SOURCE = "INVENTORY_SERVICE";

  private final JsonUtil jsonUtil;
  private final KafkaProducer producer;
  private final InventoryRepository inventoryRepository;
  private final OrderInventoryRepository orderInventoryRepository;

  public void updateInventory(Event event) {
    try {
      checkCurrentValidation(event);
      createOrderInventory(event);
      updateInventory(event.getPayload());

    } catch (Exception e) {
      log.error("Error updating inventory: ", e);
    }

    producer.sendEvent(jsonUtil.toJson(event));
  }

  private void updateInventory(Order order) {
    order
        .getProducts()
        .forEach(
            product -> {
              Inventory inventory = findInventoryByProductCode(product.getProduct().getCode());
              checkInventory(inventory.getAvailable(), product.getQuantity());

              inventory.setAvailable(inventory.getAvailable() - product.getQuantity());

              inventoryRepository.save(inventory);
            });
  }

  private void checkInventory(int available, int orderQuantity) {
    if (orderQuantity > available) {
      throw new ValidationException("Product out of stock!");
    }
  }

  private void checkCurrentValidation(Event event) {
    if (orderInventoryRepository.existsByOrderIdAndTransactionId(
        event.getPayload().getId(), event.getTransactionId())) {
      throw new ValidationException(
          "There's another transaction for this transactionId: " + event.getTransactionId());
    }
  }

  private void createOrderInventory(Event event) {

    event
        .getPayload()
        .getProducts()
        .forEach(
            product -> {
              Inventory inventory = findInventoryByProductCode(product.getProduct().getCode());
              OrderInventory orderInventory = createOrderInventory(event, product, inventory);

              orderInventoryRepository.save(orderInventory);
            });
  }

  private OrderInventory createOrderInventory(
      Event event, OrderProducts orderProducts, Inventory inventory) {
    return OrderInventory.builder()
        .inventory(inventory)
        .oldQuantity(inventory.getAvailable())
        .orderQuantity(orderProducts.getQuantity())
        .newQuantity(inventory.getAvailable() - orderProducts.getQuantity())
        .orderId(event.getPayload().getId())
        .transactionId(event.getTransactionId())
        .build();
  }

  private void handleSuccess(Event event) {
    event.setStatus(ESagaStatus.SUCCESS);
    event.setSource(CURRENT_SOURCE);

    addHistory(event, "Inventory Update Successful!");
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

  private Inventory findInventoryByProductCode(String productCode) {
    return inventoryRepository
        .findByProductCode(productCode)
        .orElseThrow(
            () ->
                new ValidationException(
                    "Inventory not found with the given productCode: " + productCode));
  }
}
