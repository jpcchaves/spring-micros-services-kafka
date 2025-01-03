package br.com.microservices.orchestrated.orchestratorservice.core.saga;

import static br.com.microservices.orchestrated.orchestratorservice.core.saga.SagaHandler.*;

import br.com.microservices.orchestrated.orchestratorservice.config.exception.ValidationException;
import br.com.microservices.orchestrated.orchestratorservice.core.dto.Event;
import br.com.microservices.orchestrated.orchestratorservice.core.enums.EEventSource;
import br.com.microservices.orchestrated.orchestratorservice.core.enums.ETopics;
import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Slf4j
@Component
@AllArgsConstructor
public class SagaExecutionController {

  private static final String SAGA_LOG_ID = "ORDER_ID: %s | TRANSACTION_ID: %s | EVENT_ID: %s";
  private static final String SUCCESS_LOG_MESSAGE =
      "### CURRENT SAGA: {} | SUCCESS | NEXT TOPIC: {} | {}";
  private static final String FAIL_LOG_MESSAGE =
      "### CURRENT SAGA: {} | FAIL SENDING TO ROLLBACK PREVIOUS SERVICE | NEXT TOPIC: {} | {}";
  private static final String ROLLBACK_LOG_MESSAGE =
      "### CURRENT SAGA: {} | SENDING TO ROLLBACK CURRENT SERVICE | NEXT TOPIC: {} | {}";

  public ETopics getNextTopic(Event event) {

    if (ObjectUtils.isEmpty(event.getStatus()) || ObjectUtils.isEmpty(event.getSource())) {
      throw new ValidationException("Source and status must be informed!");
    }

    ETopics topic = findTopicBySourceAndStatus(event);

    logCurrentSaga(event, topic);

    return topic;
  }

  private ETopics findTopicBySourceAndStatus(Event event) {
    return (ETopics)
        (Arrays.stream(SAGA_HANDLER)
            .filter(row -> isEventSourceAndStatusValid(event, row))
            .map(i -> i[TOPIC_INDEX])
            .findFirst()
            .orElseThrow(() -> new ValidationException("Topic not found!")));
  }

  private boolean isEventSourceAndStatusValid(Event event, Object[] row) {
    Object source = row[EVENT_SOURCE_INDEX];
    Object status = row[SAGA_STATUS_INDEX];

    return event.getSource().equals(source) && event.getStatus().equals(status);
  }

  private void logCurrentSaga(Event event, ETopics topic) {
    String sagaId = createSagaId(event);
    EEventSource source = event.getSource();

    switch (event.getStatus()) {
      case SUCCESS -> log.info(SUCCESS_LOG_MESSAGE, source, topic, sagaId);
      case FAIL -> log.info(FAIL_LOG_MESSAGE, source, topic, sagaId);
      case ROLLBACK_PENDING -> log.info(ROLLBACK_LOG_MESSAGE, source, topic, sagaId);
    }
  }

  private String createSagaId(Event event) {
    return String.format(
        SAGA_LOG_ID, event.getPayload().getId(), event.getTransactionId(), event.getId());
  }
}
