package br.com.microservices.orchestrated.paymentservice.core.repository;

import br.com.microservices.orchestrated.paymentservice.core.model.Payment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

  Boolean existsByOrderIdAndTransactionId(String orderId, String transactionId);

  Optional<Payment> findByOrderIdAndTransactionId(String orderId, String transactionId);
}
