package com.restaurant.restaurantapp.integration;

import static com.restaurant.restaurantapp.TestFixtures.category;
import static com.restaurant.restaurantapp.TestFixtures.menuItem;
import static com.restaurant.restaurantapp.TestFixtures.order;
import static com.restaurant.restaurantapp.TestFixtures.orderItem;
import static com.restaurant.restaurantapp.TestFixtures.payment;
import static com.restaurant.restaurantapp.TestFixtures.restaurantTable;
import static com.restaurant.restaurantapp.TestFixtures.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class OrderManagementIntegrationTest {

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
    void scenarioCompleteOrderWithMultipleItemsAndPayment() throws Exception {
        // Crează waiter
        long waiterId = createAndReturnId("/api/users", user("waiter1", "secret", "WAITER"));

        // Crează categorii și menu items
        long startersId = createAndReturnId("/api/categories", category("Starters"));
        long mainsId = createAndReturnId("/api/categories", category("Mains"));

        long soupId = createAndReturnId("/api/menu-items",
                menuItem("Soup", 8.5, categoryWithId(startersId, "Starters")));
        long steakId = createAndReturnId("/api/menu-items",
                menuItem("Steak", 25.0, categoryWithId(mainsId, "Mains")));

        // Crează masa
        long tableId = createAndReturnId("/api/restaurant-tables",
                restaurantTable(5, 4, userWithId(waiterId)));

        // Crează comandă
        long orderId = createAndReturnId("/api/orders",
                order(tableWithId(tableId, userWithId(waiterId)),
                        userWithId(waiterId),
                        "OPEN",
                        0.0));

        // Verifică că comanda are status OPEN și total 0
        mockMvc.perform(get("/api/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.totalPrice").value(0.0));

        // Adaugă două item-uri la comandă
        long orderItem1Id = createAndReturnId("/api/order-items",
                orderItem(orderWithId(orderId, tableWithId(tableId, userWithId(waiterId)), userWithId(waiterId)),
                        menuItemWithId(soupId, categoryWithId(startersId, "Starters")),
                        2));

        long orderItem2Id = createAndReturnId("/api/order-items",
                orderItem(orderWithId(orderId, tableWithId(tableId, userWithId(waiterId)), userWithId(waiterId)),
                        menuItemWithId(steakId, categoryWithId(mainsId, "Mains")),
                        1));

        // Verifică că totalul comenzii s-a actualizat (8.5*2 + 25*1 = 42.0)
        mockMvc.perform(get("/api/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPrice").value(42.0));

        // Creează plată CARD
        long paymentId = createAndReturnId("/api/payments",
                payment(orderWithId(orderId, tableWithId(tableId, userWithId(waiterId)), userWithId(waiterId)),
                        "CARD",
                        true));

        // Verifică că comanda are status PAID după plată
        mockMvc.perform(get("/api/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));

        // Verifică plata
        mockMvc.perform(get("/api/payments/{id}", paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.method").value("CARD"))
                .andExpect(jsonPath("$.paid").value(true));
    }

    @Test
    void scenarioDeleteOrderItemUpdatesOrderTotal() throws Exception {
        // Setup: waiter, categorie, menu item, masa, comandă
        long waiterId = createAndReturnId("/api/users", user("waiter2", "secret", "WAITER"));
        long categoryId = createAndReturnId("/api/categories", category("Desserts"));
        long cakeId = createAndReturnId("/api/menu-items",
                menuItem("Cake", 7.5, categoryWithId(categoryId, "Desserts")));
        long tableId = createAndReturnId("/api/restaurant-tables",
                restaurantTable(10, 2, userWithId(waiterId)));

        long orderId = createAndReturnId("/api/orders",
                order(tableWithId(tableId, userWithId(waiterId)),
                        userWithId(waiterId),
                        "OPEN",
                        0.0));

        // Adaugă item la comandă
        long orderItemId = createAndReturnId("/api/order-items",
                orderItem(orderWithId(orderId, tableWithId(tableId, userWithId(waiterId)), userWithId(waiterId)),
                        menuItemWithId(cakeId, categoryWithId(categoryId, "Desserts")),
                        3));

        // Verifică total: 7.5 * 3 = 22.5
        mockMvc.perform(get("/api/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPrice").value(22.5));

        // Șterge order item
        mockMvc.perform(delete("/api/order-items/{id}", orderItemId))
                .andExpect(status().isNoContent());

        // Verifică că totalul s-a resetat la 0
        mockMvc.perform(get("/api/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPrice").value(0.0));
    }

    @Test
    void scenarioUpdatePaymentStatusAffectsOrderStatus() throws Exception {
        // Setup
        long waiterId = createAndReturnId("/api/users", user("waiter3", "secret", "WAITER"));
        long categoryId = createAndReturnId("/api/categories", category("Beverages"));
        long coffeeId = createAndReturnId("/api/menu-items",
                menuItem("Coffee", 3.5, categoryWithId(categoryId, "Beverages")));
        long tableId = createAndReturnId("/api/restaurant-tables",
                restaurantTable(15, 2, userWithId(waiterId)));

        long orderId = createAndReturnId("/api/orders",
                order(tableWithId(tableId, userWithId(waiterId)),
                        userWithId(waiterId),
                        "OPEN",
                        0.0));

        // Crează plată inițial neplătită
        long paymentId = createAndReturnId("/api/payments",
                payment(orderWithId(orderId, tableWithId(tableId, userWithId(waiterId)), userWithId(waiterId)),
                        "CASH",
                        false));

        // Verifică că comanda rămâne OPEN
        mockMvc.perform(get("/api/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));

        // Actualizează plata la PAID
        mockMvc.perform(put("/api/payments/{id}", paymentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJson(payment(orderWithId(orderId, tableWithId(tableId, userWithId(waiterId)), userWithId(waiterId)),
                        "CASH", true))))
                .andExpect(status().isOk());

        // Verifică că comanda are status PAID
        mockMvc.perform(get("/api/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));

        // Actualizează plata înapoi la NOT PAID
        mockMvc.perform(put("/api/payments/{id}", paymentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJson(payment(orderWithId(orderId, tableWithId(tableId, userWithId(waiterId)), userWithId(waiterId)),
                        "CASH", false))))
                .andExpect(status().isOk());

        // Verifică că comanda revine la OPEN
        mockMvc.perform(get("/api/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));
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
        com.restaurant.restaurantapp.model.Order order = order(table, waiter, "OPEN", 0.0);
        order.setId(id);
        return order;
    }

    private static com.restaurant.restaurantapp.model.MenuItem menuItemWithId(Long id,
            com.restaurant.restaurantapp.model.Category category) {
        com.restaurant.restaurantapp.model.MenuItem menuItem = menuItem("Item", 0.0, category);
        menuItem.setId(id);
        return menuItem;
    }
}

