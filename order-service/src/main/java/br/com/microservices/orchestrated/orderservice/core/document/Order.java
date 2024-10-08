package br.com.microservices.orchestrated.orderservice.core.document;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

  private String id;
  private List<Product> products;
  private LocalDateTime createdAt;
  private String transactionId;
  private double totalAmount;
  private int totalItems;
}
