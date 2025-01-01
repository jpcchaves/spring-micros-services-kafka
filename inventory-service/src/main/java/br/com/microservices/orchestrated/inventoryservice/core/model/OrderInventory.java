package br.com.microservices.orchestrated.inventoryservice.core.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Builder
@Entity
@Table(name = "order_inventory")
public class OrderInventory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String orderId;

  @Column(nullable = false)
  private String transactionId;

  @Column(nullable = false)
  private Integer orderQuantity;

  @Column(nullable = false)
  private Integer oldQuantity;

  @Column(nullable = false)
  private Integer newQuantity;

  @ManyToOne
  @JoinColumn(
      name = "inventory_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "inventory_fk", value = ConstraintMode.CONSTRAINT))
  private Inventory inventory;

  @CreationTimestamp
  @Column(nullable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(nullable = false)
  private LocalDateTime updatedAt;
}
