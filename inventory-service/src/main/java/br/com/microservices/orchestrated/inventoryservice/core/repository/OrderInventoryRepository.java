package br.com.microservices.orchestrated.inventoryservice.core.repository;

import br.com.microservices.orchestrated.inventoryservice.core.model.OrderInventory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderInventoryRepository extends JpaRepository<OrderInventory, Long> {

  Boolean existsByOrderIdAndTransactionId(String orderId, String transactionId);

  List<OrderInventory> findByOrderIdAndTransactionId(String orderId, String transactionId);
}
