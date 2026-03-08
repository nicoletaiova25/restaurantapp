package com.restaurant.restaurantapp.service;

import com.restaurant.restaurantapp.exception.BadRequestException;
import com.restaurant.restaurantapp.exception.ResourceNotFoundException;
import com.restaurant.restaurantapp.model.RestaurantTable;
import com.restaurant.restaurantapp.repository.RestaurantTableRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RestaurantTableService {

    private final RestaurantTableRepository restaurantTableRepository;

    public RestaurantTableService(RestaurantTableRepository restaurantTableRepository) {
        this.restaurantTableRepository = restaurantTableRepository;
    }

    public List<RestaurantTable> getAllTables() {
        return restaurantTableRepository.findAll();
    }

    public RestaurantTable getTableById(Long id) {
        validateId(id);
        return restaurantTableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant table", "id", id));
    }

    public RestaurantTable createTable(RestaurantTable restaurantTable) {
        validatePayload(restaurantTable, "Restaurant table payload is required");
        return restaurantTableRepository.save(restaurantTable);
    }

    public RestaurantTable updateTable(Long id, RestaurantTable updatedTable) {
        validatePayload(updatedTable, "Updated restaurant table payload is required");
        RestaurantTable table = getTableById(id);
        table.setTableNumber(updatedTable.getTableNumber());
        table.setSeats(updatedTable.getSeats());
        table.setWaiter(updatedTable.getWaiter());
        return restaurantTableRepository.save(table);
    }

    public void deleteTable(Long id) {
        validateId(id);
        if (!restaurantTableRepository.existsById(id)) {
            throw new ResourceNotFoundException("Restaurant table", "id", id);
        }
        restaurantTableRepository.deleteById(id);
    }

    private void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new BadRequestException("Id must be a positive number");
        }
    }

    private void validatePayload(Object payload, String message) {
        if (payload == null) {
            throw new BadRequestException(message);
        }
    }
}
