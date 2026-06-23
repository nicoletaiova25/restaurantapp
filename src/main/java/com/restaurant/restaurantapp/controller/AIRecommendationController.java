package com.restaurant.restaurantapp.controller;

import com.restaurant.restaurantapp.model.MenuItem;
import com.restaurant.restaurantapp.repository.MenuItemRepository;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
public class AIRecommendationController {

    private final MenuItemRepository menuItemRepository;

    public AIRecommendationController(MenuItemRepository menuItemRepository) {
        this.menuItemRepository = menuItemRepository;
    }

    @GetMapping("/api/ai/recommendations")
    public List<String> getRecommendations() {

        List<MenuItem> items = menuItemRepository.findAll();

        if (items.isEmpty()) {
            return List.of(
                    "AI: Nu există produse în meniu. Adaugă produse pentru a genera recomandări.",
                    "AI: Creează categorii precum desserts, drinks sau main course pentru recomandări mai bune."
            );
        }

        return items.stream()
                .sorted(Comparator.comparingDouble(this::calculateScore).reversed())
                .limit(5)
                .map(item -> {
                    double score = calculateScore(item);

                    return "BonjourAi" + String.format("%.1f", score)
                            + " → Recomandă produsul: "
                            + item.getName()
                            + " | Preț: "
                            + item.getPrice()
                            + " | Categorie: "
                            + (item.getCategory() != null ? item.getCategory().getName() : "fără categorie");
                })
                .toList();
    }

    private double calculateScore(MenuItem item) {
        double score = 0;

        score += item.getPrice() * 2;

        if (item.getCategory() != null) {
            String category = item.getCategory().getName().toLowerCase();

            if (category.contains("desert") || category.contains("dessert")) {
                score += 30;
            }

            if (category.contains("drink") || category.contains("bautura") || category.contains("coffee")) {
                score += 20;
            }

            if (category.contains("main") || category.contains("mancare")) {
                score += 15;
            }
        }

        return score;
    }
}