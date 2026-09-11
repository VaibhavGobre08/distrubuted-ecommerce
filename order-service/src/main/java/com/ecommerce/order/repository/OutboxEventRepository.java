package com.ecommerce.order.repository;

import com.ecommerce.order.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxEventRepository
        extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findByStatusAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(
            String status,
            LocalDateTime time
    );
}