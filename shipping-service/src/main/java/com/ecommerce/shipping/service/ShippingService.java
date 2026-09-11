package com.ecommerce.shipping.service;

import com.ecommerce.shipping.dto.CreateShippingRequest;
import com.ecommerce.shipping.entity.Shipping;
import com.ecommerce.shipping.entity.ShippingStatus;
import com.ecommerce.shipping.repository.ShippingRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ShippingService {

    private final ShippingRepository shippingRepository;

    public ShippingService(ShippingRepository shippingRepository) {
        this.shippingRepository = shippingRepository;
    }

    public Shipping createShipping(CreateShippingRequest request) {

        Shipping shipping = new Shipping();

        shipping.setOrderId(request.orderId());
        shipping.setCustomerId(request.customerId());
        shipping.setAddress(request.address());

        // Initial status
        shipping.setStatus(ShippingStatus.PENDING);

        shipping.setCreatedAt(LocalDateTime.now());

        return shippingRepository.save(shipping);
    }

    public Shipping getShipping(Long id) {

        return shippingRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Shipment not found: " + id));
    }
}