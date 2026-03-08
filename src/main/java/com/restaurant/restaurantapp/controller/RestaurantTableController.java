package com.restaurant.restaurantapp.controller;

import com.restaurant.restaurantapp.model.RestaurantTable;
import com.restaurant.restaurantapp.service.RestaurantTableService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/restaurant-tables")
public class RestaurantTableController {

    private final RestaurantTableService restaurantTableService;

    public RestaurantTableController(RestaurantTableService restaurantTableService) {
        this.restaurantTableService = restaurantTableService;
    }

    @GetMapping
    public List<RestaurantTable> getAllTables() {
        return restaurantTableService.getAllTables();
    }

    @GetMapping("/{id}")
    public RestaurantTable getTableById(@PathVariable Long id) {
        return restaurantTableService.getTableById(id);
    }

    @PostMapping
    public ResponseEntity<RestaurantTable> createTable(@Valid @RequestBody RestaurantTable table) {
        return new ResponseEntity<>(restaurantTableService.createTable(table), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public RestaurantTable updateTable(@PathVariable Long id, @Valid @RequestBody RestaurantTable table) {
        return restaurantTableService.updateTable(id, table);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTable(@PathVariable Long id) {
        restaurantTableService.deleteTable(id);
        return ResponseEntity.noContent().build();
    }
}
