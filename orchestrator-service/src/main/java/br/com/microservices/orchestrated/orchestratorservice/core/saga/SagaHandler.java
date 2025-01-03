package br.com.microservices.orchestrated.orchestratorservice.core.saga;

import static br.com.microservices.orchestrated.orchestratorservice.core.enums.EEventSource.*;
import static br.com.microservices.orchestrated.orchestratorservice.core.enums.ESagaStatus.*;
import static br.com.microservices.orchestrated.orchestratorservice.core.enums.ETopics.*;

public final class SagaHandler {

    private SagaHandler() {
        //
    }

    public static final Object[][] SAGA_HANDLER = {

            /*  START_SAGA */

            // If success, go to product validation success topic
            {ORCHESTRATOR, SUCCESS, PRODUCT_VALIDATION_SUCCESS},

            // If fails, go to finish fail topic
            {ORCHESTRATOR, FAIL, FINISH_FAIL},

            /* Product Validation cases */
            // If error, go to product validation fail topic to roll back the operation
            {PRODUCT_VALIDATION_SERVICE, ROLLBACK_PENDING, PRODUCT_VALIDATION_FAIL},

            // After roll back the operation, go to finish fail topic
            {PRODUCT_VALIDATION_SERVICE, FAIL, FINISH_FAIL},

            // If success, go to payment success topic
            {PRODUCT_VALIDATION_SERVICE, SUCCESS, PAYMENT_SUCCESS},

            /* Payment Service cases */
            // If error, roll back and go to itself topic payment fail
            {PAYMENT_SERVICE, ROLLBACK_PENDING, PAYMENT_FAIL},

            // Roll back the operation and go to the prev topic product validation fail
            {PAYMENT_SERVICE, FAIL, PRODUCT_VALIDATION_FAIL},

            // If success, go to inventory success topic
            {PAYMENT_SERVICE, SUCCESS, INVENTORY_SUCCESS},


            /* Inventory Service cases */
            // If error, roll back and go to itself topic inventory fail
            {INVENTORY_SERVICE, ROLLBACK_PENDING, INVENTORY_FAIL},

            // After roll back, go to prev topic payment fail
            {INVENTORY_SERVICE, FAIL, PAYMENT_FAIL},

            // If success, finishes saga with finish success topic
            {INVENTORY_SERVICE, SUCCESS, FINISH_SUCCESS},
    };

    public static final int EVENT_SOURCE_INDEX = 0;
    public static final int SAGA_STATUS_INDEX = 1;
    public static final int TOPIC_INDEX = 2;
}
