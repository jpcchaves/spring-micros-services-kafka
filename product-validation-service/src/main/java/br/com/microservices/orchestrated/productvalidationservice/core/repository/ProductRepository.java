package br.com.microservices.orchestrated.productvalidationservice.core.repository;

import br.com.microservices.orchestrated.productvalidationservice.core.dto.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

  Boolean existsByCode(String code);
}
