package br.com.microservices.orchestrated.inventoryservice.core.repository;

import br.com.microservices.orchestrated.inventoryservice.core.model.Inventory;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

  Optional<Inventory> findByProductCode(String productCode);
}
