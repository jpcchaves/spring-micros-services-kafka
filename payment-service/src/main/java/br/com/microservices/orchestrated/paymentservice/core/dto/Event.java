package br.com.microservices.orchestrated.paymentservice.core.dto;

import br.com.microservices.orchestrated.paymentservice.core.enums.ESagaStatus;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.ObjectUtils;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Event {

  private String id;
  private String transactionId;
  private String orderId;
  private Order payload;
  private String source;
  private ESagaStatus status;
  private List<History> eventHistory;
  private LocalDateTime createdAt;

  public void addToHistory(History history) {
    if (ObjectUtils.isEmpty(eventHistory)) {
      eventHistory = new ArrayList<>();
    }

    eventHistory.add(history);
  }
}
