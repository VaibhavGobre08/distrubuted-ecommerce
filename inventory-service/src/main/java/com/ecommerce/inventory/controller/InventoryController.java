package com.ecommerce.inventory.controller;

import com.ecommerce.inventory.dto.CreateProductRequest;
import com.ecommerce.inventory.entity.Product;
import com.ecommerce.inventory.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product createProduct(
            @Valid @RequestBody CreateProductRequest request) {

        return inventoryService.createProduct(request);
    }

    @GetMapping
    public List<Product> getAllProducts() {
        return inventoryService.getAllProducts();
    }

    @GetMapping("/{id}")
    public Product getProduct(@PathVariable Long id) {
        return inventoryService.getProduct(id);
    }
}