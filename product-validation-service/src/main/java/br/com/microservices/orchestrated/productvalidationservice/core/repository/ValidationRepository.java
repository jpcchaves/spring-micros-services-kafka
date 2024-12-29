package br.com.microservices.orchestrated.productvalidationservice.core.repository;

import br.com.microservices.orchestrated.productvalidationservice.core.model.Validation;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ValidationRepository extends JpaRepository<Validation, Long> {

  Boolean existsByOrderIdAndTransactionId(String orderId, String transactionId);

  Optional<Validation> findByOrderIdAndTransactionId(String orderId, String transactionId);
}
