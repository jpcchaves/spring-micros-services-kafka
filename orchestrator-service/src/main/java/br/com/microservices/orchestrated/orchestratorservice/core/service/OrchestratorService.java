package br.com.microservices.orchestrated.orchestratorservice.core.service;

import br.com.microservices.orchestrated.orchestratorservice.core.dto.Event;
import br.com.microservices.orchestrated.orchestratorservice.core.dto.History;
import br.com.microservices.orchestrated.orchestratorservice.core.enums.EEventSource;
import br.com.microservices.orchestrated.orchestratorservice.core.enums.ESagaStatus;
import br.com.microservices.orchestrated.orchestratorservice.core.enums.ETopics;
import br.com.microservices.orchestrated.orchestratorservice.core.producer.SagaOrchestratorProducer;
import br.com.microservices.orchestrated.orchestratorservice.core.saga.SagaExecutionController;
import br.com.microservices.orchestrated.orchestratorservice.core.utils.JsonUtil;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class OrchestratorService {

  private final JsonUtil jsonUtil;
  private final SagaExecutionController sagaExecutionController;
  private final SagaOrchestratorProducer sagaOrchestratorProducer;

  public void startSaga(Event event) {
    event.setSource(EEventSource.ORCHESTRATOR);
    event.setStatus(ESagaStatus.SUCCESS);

    ETopics topic = getTopic(event);
    log.info("SAGA STARTED");
    addHistory(event, "Saga started!");
    produceEvent(event, topic);
  }

  public void finishSagaSuccess(Event event) {
    event.setSource(EEventSource.ORCHESTRATOR);
    event.setStatus(ESagaStatus.SUCCESS);

    log.info("SAGA FINISHED SUCCESSFULLY FOR EVENT {}", event.getId());
    addHistory(event, "Saga finished successfully!");
    notifyFinishedSaga(event);
  }

  public void finishSagaFail(Event event) {
    event.setSource(EEventSource.ORCHESTRATOR);
    event.setStatus(ESagaStatus.FAIL);

    log.info("SAGA FINISHED WITH FAILS FOR EVENT {}", event.getId());
    addHistory(event, "Saga finished with fails!");
    notifyFinishedSaga(event);
  }

  public void continueSaga(Event event) {
    ETopics topic = getTopic(event);
    log.info("CONTINUING SAGA FOR EVENT {}", event.getId());
    produceEvent(event, topic);
  }

  private ETopics getTopic(Event event) {
    return sagaExecutionController.getNextTopic(event);
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

  private void notifyFinishedSaga(Event event) {
    produceEvent(event, ETopics.NOTIFY_ENDING);
  }

  private void produceEvent(Event event, ETopics topic) {
    sagaOrchestratorProducer.sendEvent(jsonUtil.toJson(event), topic.getTopic());
  }
}
