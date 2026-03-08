package com.restaurant.restaurantapp.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Entity
@Data
public class MenuItem {

    @Id
    @GeneratedValue
    private Long id;

    @NotBlank(message = "Menu item name is required")
    private String name;

    @Positive(message = "Price must be greater than 0")
    private double price;

    @NotNull(message = "Category is required")
    @ManyToOne
    private Category category;
}