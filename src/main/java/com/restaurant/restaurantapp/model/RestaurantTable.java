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
public class RestaurantTable {

    @Id
    @GeneratedValue
    private Long id;

    @Positive(message = "Table number must be greater than 0")
    private int tableNumber;

    @Positive(message = "Seats must be greater than 0")
    private int seats;

    @NotNull(message = "Waiter is required")
    @ManyToOne
    private User waiter;
}
