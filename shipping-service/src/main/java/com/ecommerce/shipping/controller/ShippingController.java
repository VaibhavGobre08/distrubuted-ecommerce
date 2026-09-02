package com.ecommerce.shipping.controller;

import com.ecommerce.shipping.dto.CreateShippingRequest;
import com.ecommerce.shipping.entity.Shipping;
import com.ecommerce.shipping.service.ShippingService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shipping")
public class ShippingController {

    private final ShippingService shippingService;

    public ShippingController(ShippingService shippingService) {
        this.shippingService = shippingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Shipping createShipping(
            @Valid @RequestBody CreateShippingRequest request) {

        return shippingService.createShipping(request);
    }

    @GetMapping("/{id}")
    public Shipping getShipping(@PathVariable Long id) {

        return shippingService.getShipping(id);
    }
}