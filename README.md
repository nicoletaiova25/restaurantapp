# Restaurant App

## ER Diagram

The diagram below documents the current entity relationships used in this project.

## Mermaid Plugin (IntelliJ)

To render Mermaid diagrams directly in IntelliJ IDEA Community Edition:

1. Open `Settings` (`File -> Settings` on Windows/Linux).
2. Go to `Plugins -> Marketplace`.
3. Search for `Mermaid` and install a Mermaid preview plugin.
4. Restart IntelliJ.
5. Open `README.md` and use Markdown preview to view the ER diagram.

If your plugin supports it, enable options like `Auto-render` or `Render on save` for smoother editing.

```mermaid
erDiagram
    USER {
        Long id PK
        String username
        String password
        String role
    }

    CATEGORY {
        Long id PK
        String name
    }

    MENU_ITEM {
        Long id PK
        String name
        double price
        Long category_id FK
    }

    RESTAURANT_TABLE {
        Long id PK
        int tableNumber
        int seats
        Long waiter_id FK
    }

    ORDERS {
        Long id PK
        Long table_id FK
        Long waiter_id FK
        String status
        double totalPrice
    }

    ORDER_ITEM {
        Long id PK
        Long order_id FK
        Long menu_item_id FK
        int quantity
    }

    PAYMENT {
        Long id PK
        Long order_id FK
        String method
        boolean paid
    }

    CATEGORY ||--o{ MENU_ITEM : contains
    USER ||--o{ RESTAURANT_TABLE : assigned_to
    USER ||--o{ ORDERS : serves
    RESTAURANT_TABLE ||--o{ ORDERS : has
    ORDERS ||--o{ ORDER_ITEM : includes
    MENU_ITEM ||--o{ ORDER_ITEM : referenced_by
    ORDERS ||--|| PAYMENT : paid_by
```

## Notes

- `Order` is mapped to table name `orders` in code.
- `Payment` is a one-to-one relation with `Order`.
- `OrderItem` acts as the line-item bridge between `Order` and `MenuItem`.
