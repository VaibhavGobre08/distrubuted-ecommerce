package com.ecommerce.order.outbox;

import com.ecommerce.order.entity.OutboxEvent;
import com.ecommerce.order.repository.OutboxEventRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class OutboxEventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    
    private static final String OUTBOX_DLT_TOPIC = "order.created.dlt";

    public OutboxEventPublisher(
            OutboxEventRepository outboxEventRepository,
            KafkaTemplate<String, String> kafkaTemplate) {

        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 5000)
    public void publishPendingEvents() {

    	List<OutboxEvent> events =
    	        outboxEventRepository
    	                .findByStatusAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(
    	                        "PENDING",
    	                        LocalDateTime.now()
    	                );

        for (OutboxEvent event : events) {
            publishEvent(event);
        }
    }

    @Transactional
    public void publishEvent(OutboxEvent event) {

        try {

            kafkaTemplate.send(
                    "order.created",
                    event.getAggregateId(),
                    event.getPayload()
            ).get();

            event.setStatus("PUBLISHED");
            event.setPublishedAt(LocalDateTime.now());

            outboxEventRepository.save(event);

            System.out.println("=================================");
            System.out.println("Outbox event published successfully");
            System.out.println("Event ID: " + event.getId());
            System.out.println("Aggregate ID: " + event.getAggregateId());
            System.out.println("=================================");

        } catch (Exception e) {

            int retryCount = event.getRetryCount() + 1;

            event.setRetryCount(retryCount);

            if (retryCount >= 3) {

                try {
                    kafkaTemplate.send(
                            OUTBOX_DLT_TOPIC,
                            event.getAggregateId(),
                            event.getPayload()
                    ).get();

                    event.setStatus("FAILED");

                    System.out.println("=================================");
                    System.out.println("Event moved to DLT");
                    System.out.println("Event ID: " + event.getId());
                    System.out.println("Topic: " + OUTBOX_DLT_TOPIC);
                    System.out.println("=================================");

                } catch (Exception dltException) {

                    System.out.println("=================================");
                    System.out.println("FAILED TO PUBLISH TO DLT");
                    System.out.println("Event ID: " + event.getId());
                    System.out.println("Error: " + dltException.getMessage());
                    System.out.println("=================================");
                }

            } else {

                long delaySeconds =
                        (long) Math.pow(2, retryCount);

                event.setNextRetryAt(
                        LocalDateTime.now()
                                .plusSeconds(delaySeconds)
                );

                System.out.println("Retry scheduled");
                System.out.println("Retry count: " + retryCount);
            }

            outboxEventRepository.save(event);
        }
    }
}