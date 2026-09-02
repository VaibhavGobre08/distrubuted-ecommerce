package com.ecommerce.shipping.repository;

import com.ecommerce.shipping.entity.Shipping;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ShippingRepository extends JpaRepository<Shipping, Long> {

}