package com.restaurant.restaurantapp.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Entity
@Data
public class Payment {

    @Id
    @GeneratedValue
    private Long id;

    @NotNull(message = "Order is required")
    @OneToOne
    private Order order;

    @NotBlank(message = "Payment method is required")
    private String method;

    private boolean paid;
}