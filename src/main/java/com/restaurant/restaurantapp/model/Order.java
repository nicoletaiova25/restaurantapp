package com.restaurant.restaurantapp.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Entity
@Data
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue
    private Long id;

    @NotNull(message = "Table is required")
    @ManyToOne
    private RestaurantTable table;

    @NotNull(message = "Waiter is required")
    @ManyToOne
    private User waiter;

    private String status = "OPEN";

    private double totalPrice = 0.0;
}