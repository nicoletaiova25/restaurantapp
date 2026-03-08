package com.restaurant.restaurantapp.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Entity
@Data
public class OrderItem {

    @Id
    @GeneratedValue
    private Long id;

    @NotNull(message = "Order is required")
    @ManyToOne
    private Order order;

    @NotNull(message = "Menu item is required")
    @ManyToOne
    private MenuItem menuItem;

    @Positive(message = "Quantity must be greater than 0")
    private int quantity;
}
