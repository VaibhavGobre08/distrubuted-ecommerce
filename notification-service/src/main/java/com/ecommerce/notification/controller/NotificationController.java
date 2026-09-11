package com.ecommerce.notification.controller;

import com.ecommerce.notification.entity.Notification;
import com.ecommerce.notification.repository.NotificationRepository;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    public NotificationController(
            NotificationRepository notificationRepository) {

        this.notificationRepository = notificationRepository;
    }

    @GetMapping("/order/{orderId}")
    public List<Notification> getByOrder(
            @PathVariable Long orderId) {

        return notificationRepository
                .findByOrderId(orderId);
    }
}