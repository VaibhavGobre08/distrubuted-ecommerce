package com.ecommerce.notification.service;

import com.ecommerce.notification.entity.Notification;
import com.ecommerce.notification.repository.NotificationRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(
            NotificationRepository notificationRepository) {

        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public Notification createNotification(
            Long orderId,
            String username,
            String message) {

        Notification notification = new Notification();

        notification.setOrderId(orderId);
        notification.setUsername(username);
        notification.setMessage(message);
        notification.setStatus("SENT");
        notification.setCreatedAt(LocalDateTime.now());

        return notificationRepository.save(notification);
    }
}