package com.restaurant.restaurantapp.integration;

import static com.restaurant.restaurantapp.TestFixtures.category;
import static com.restaurant.restaurantapp.TestFixtures.menuItem;
import static com.restaurant.restaurantapp.TestFixtures.order;
import static com.restaurant.restaurantapp.TestFixtures.orderItem;
import static com.restaurant.restaurantapp.TestFixtures.payment;
import static com.restaurant.restaurantapp.TestFixtures.restaurantTable;
import static com.restaurant.restaurantapp.TestFixtures.user;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.restaurant.restaurantapp.repository.CategoryRepository;
import com.restaurant.restaurantapp.repository.MenuItemRepository;
import com.restaurant.restaurantapp.repository.OrderItemRepository;
import com.restaurant.restaurantapp.repository.OrderRepository;
import com.restaurant.restaurantapp.repository.PaymentRepository;
import com.restaurant.restaurantapp.repository.RestaurantTableRepository;
import com.restaurant.restaurantapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class ApiIntegrationTest {

    private static final Pattern ID_PATTERN = Pattern.compile("\"id\"\\s*:\\s*(\\d+)");

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private RestaurantTableRepository restaurantTableRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        paymentRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        menuItemRepository.deleteAll();
        restaurantTableRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void createsCategoryAndMenuItemEndToEnd() throws Exception {
        long categoryId = createAndReturnId("/api/categories", category("Starters"));

        long menuItemId = createAndReturnId("/api/menu-items",
                menuItem("Soup", 12.5, categoryWithId(categoryId, "Starters")));

        mockMvc.perform(get("/api/menu-items/{id}", menuItemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(menuItemId))
                .andExpect(jsonPath("$.name").value("Soup"))
                .andExpect(jsonPath("$.category.id").value(categoryId))
                .andExpect(jsonPath("$.category.name").value("Starters"));
    }

    @Test
    void createsUserTableOrderOrderItemAndPaymentEndToEnd() throws Exception {
        long userId = createAndReturnId("/api/users", user("waiter", "secret", "WAITER"));
        long categoryId = createAndReturnId("/api/categories", category("Mains"));
        long menuItemId = createAndReturnId("/api/menu-items",
                menuItem("Steak", 24.99, categoryWithId(categoryId, "Mains")));

        long tableId = createAndReturnId("/api/restaurant-tables",
                restaurantTable(4, 2, userWithId(userId)));

        long orderId = createAndReturnId("/api/orders",
                order(tableWithId(tableId, userWithId(userId)),
                        userWithId(userId),
                        "OPEN",
                        24.99));

        long orderItemId = createAndReturnId("/api/order-items",
                orderItem(orderWithId(orderId, tableWithId(tableId, userWithId(userId)), userWithId(userId)),
                        menuItemWithId(menuItemId, categoryWithId(categoryId, "Mains")),
                        1));

        long paymentId = createAndReturnId("/api/payments",
                payment(orderWithId(orderId, tableWithId(tableId, userWithId(userId)), userWithId(userId)),
                        "CARD",
                        true));

        mockMvc.perform(get("/api/payments/{id}", paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paymentId))
                .andExpect(jsonPath("$.method").value("CARD"))
                .andExpect(jsonPath("$.paid").value(true))
                .andExpect(jsonPath("$.order.id").value(orderId));

        mockMvc.perform(get("/api/order-items/{id}", orderItemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.menuItem.id").value(menuItemId))
                .andExpect(jsonPath("$.quantity").value(1));
    }

    @Test
    void returnsGlobalErrorsForDuplicateUserMissingResourceAndValidationFailure() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(user("duplicate", "secret", "WAITER"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(user("duplicate", "secret", "WAITER"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message", containsString("Username already exists")));

        mockMvc.perform(get("/api/categories/{id}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message", containsString("Category not found with id: 999999")));

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(category(""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", containsString("Category name is required")));
    }

    private long createAndReturnId(String url, Object payload) throws Exception {
        MvcResult result = mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(payload)))
                .andExpect(status().isCreated())
                .andReturn();
        return extractId(result.getResponse().getContentAsString());
    }

    private long extractId(String body) {
        Matcher matcher = ID_PATTERN.matcher(body);
        if (!matcher.find()) {
            throw new IllegalStateException("Unable to extract id from response: " + body);
        }
        return Long.parseLong(matcher.group(1));
    }

    private String asJson(Object value) {
        if (value instanceof com.restaurant.restaurantapp.model.Category categoryValue) {
            return categoryValue.getId() == null
                    ? "{\"name\":\"" + categoryValue.getName() + "\"}"
                    : "{\"id\":" + categoryValue.getId() + "}";
        }
        if (value instanceof com.restaurant.restaurantapp.model.User userValue) {
            return userValue.getId() == null
                    ? "{\"username\":\"" + userValue.getUsername() + "\",\"password\":\"" + userValue.getPassword()
                            + "\",\"role\":\"" + userValue.getRole() + "\"}"
                    : "{\"id\":" + userValue.getId() + "}";
        }
        if (value instanceof com.restaurant.restaurantapp.model.RestaurantTable tableValue) {
            return tableValue.getId() == null
                    ? "{\"tableNumber\":" + tableValue.getTableNumber() + ",\"seats\":" + tableValue.getSeats()
                            + ",\"waiter\":" + asJson(tableValue.getWaiter()) + "}"
                    : "{\"id\":" + tableValue.getId() + "}";
        }
        if (value instanceof com.restaurant.restaurantapp.model.MenuItem menuItemValue) {
            return menuItemValue.getId() == null
                    ? "{\"name\":\"" + menuItemValue.getName() + "\",\"price\":" + menuItemValue.getPrice()
                            + ",\"category\":" + asJson(menuItemValue.getCategory()) + "}"
                    : "{\"id\":" + menuItemValue.getId() + "}";
        }
        if (value instanceof com.restaurant.restaurantapp.model.Order orderValue) {
            return orderValue.getId() == null
                    ? "{\"table\":" + asJson(orderValue.getTable()) + ",\"waiter\":" + asJson(orderValue.getWaiter())
                            + ",\"status\":\"" + orderValue.getStatus() + "\",\"totalPrice\":" + orderValue.getTotalPrice()
                            + "}"
                    : "{\"id\":" + orderValue.getId() + "}";
        }
        if (value instanceof com.restaurant.restaurantapp.model.OrderItem orderItemValue) {
            return orderItemValue.getId() == null
                    ? "{\"order\":" + asJson(orderItemValue.getOrder()) + ",\"menuItem\":" + asJson(orderItemValue.getMenuItem())
                            + ",\"quantity\":" + orderItemValue.getQuantity() + "}"
                    : "{\"id\":" + orderItemValue.getId() + "}";
        }
        if (value instanceof com.restaurant.restaurantapp.model.Payment paymentValue) {
            return paymentValue.getId() == null
                    ? "{\"order\":" + asJson(paymentValue.getOrder()) + ",\"method\":\"" + paymentValue.getMethod()
                            + "\",\"paid\":" + paymentValue.isPaid() + "}"
                    : "{\"id\":" + paymentValue.getId() + "}";
        }
        throw new IllegalArgumentException("Unsupported payload type: " + value.getClass());
    }

    private static com.restaurant.restaurantapp.model.Category categoryWithId(Long id, String name) {
        com.restaurant.restaurantapp.model.Category category = category(name);
        category.setId(id);
        return category;
    }

    private static com.restaurant.restaurantapp.model.User userWithId(Long id) {
        com.restaurant.restaurantapp.model.User user = user("waiter", "secret", "WAITER");
        user.setId(id);
        return user;
    }

    private static com.restaurant.restaurantapp.model.RestaurantTable tableWithId(Long id,
            com.restaurant.restaurantapp.model.User waiter) {
        com.restaurant.restaurantapp.model.RestaurantTable restaurantTable = restaurantTable(4, 2, waiter);
        restaurantTable.setId(id);
        return restaurantTable;
    }

    private static com.restaurant.restaurantapp.model.Order orderWithId(Long id,
            com.restaurant.restaurantapp.model.RestaurantTable table,
            com.restaurant.restaurantapp.model.User waiter) {
        com.restaurant.restaurantapp.model.Order order = order(table, waiter, "OPEN", 24.99);
        order.setId(id);
        return order;
    }

    private static com.restaurant.restaurantapp.model.MenuItem menuItemWithId(Long id,
            com.restaurant.restaurantapp.model.Category category) {
        com.restaurant.restaurantapp.model.MenuItem menuItem = menuItem("Steak", 24.99, category);
        menuItem.setId(id);
        return menuItem;
    }
}

