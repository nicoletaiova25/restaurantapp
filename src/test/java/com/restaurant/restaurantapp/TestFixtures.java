package com.restaurant.restaurantapp;

import com.restaurant.restaurantapp.model.Category;
import com.restaurant.restaurantapp.model.MenuItem;
import com.restaurant.restaurantapp.model.Order;
import com.restaurant.restaurantapp.model.OrderItem;
import com.restaurant.restaurantapp.model.Payment;
import com.restaurant.restaurantapp.model.RestaurantTable;
import com.restaurant.restaurantapp.model.User;

public final class TestFixtures {

    private TestFixtures() {
    }

    public static Category category(String name) {
        Category category = new Category();
        category.setName(name);
        return category;
    }

    public static User user(String username, String password, String role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setRole(role);
        return user;
    }

    public static RestaurantTable restaurantTable(int tableNumber, int seats, User waiter) {
        RestaurantTable restaurantTable = new RestaurantTable();
        restaurantTable.setTableNumber(tableNumber);
        restaurantTable.setSeats(seats);
        restaurantTable.setWaiter(waiter);
        return restaurantTable;
    }

    public static MenuItem menuItem(String name, double price, Category category) {
        MenuItem menuItem = new MenuItem();
        menuItem.setName(name);
        menuItem.setPrice(price);
        menuItem.setCategory(category);
        return menuItem;
    }

    public static Order order(RestaurantTable table, User waiter, String status, double totalPrice) {
        Order order = new Order();
        order.setTable(table);
        order.setWaiter(waiter);
        order.setStatus(status);
        order.setTotalPrice(totalPrice);
        return order;
    }

    public static OrderItem orderItem(Order order, MenuItem menuItem, int quantity) {
        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setMenuItem(menuItem);
        orderItem.setQuantity(quantity);
        return orderItem;
    }

    public static Payment payment(Order order, String method, boolean paid) {
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setMethod(method);
        payment.setPaid(paid);
        return payment;
    }
}

