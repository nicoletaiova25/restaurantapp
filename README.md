# Restaurant App

Restaurant App is a full-stack restaurant management application developed using Spring Boot and React.

The application allows restaurant staff to manage:

* users and roles;
* menu categories;
* menu items;
* restaurant tables;
* orders;
* order items;
* payments.

The project includes a React frontend, a Spring Boot backend, database persistence, testing, CI/CD pipeline, monitoring, microservice infrastructure and AI-assisted features.

---

<details>
<summary><h2>📋 Project Description</h2></summary>

The main purpose of the application is to manage restaurant operations in a simple and organized way.

The system supports role-based access:

* ADMIN and MANAGER can access all functionalities;
* WAITER can create orders, add products to orders and manage payments;
* BARTENDER can view orders and order items.

Tables can be assigned to waiters, and waiters can see only the orders related to their assigned tables.

</details>

---

<details>
<summary><h2>🏗️ Architecture</h2></summary>

The application started as a Spring Boot monolithic application and was later extended with microservice-related components.

Main components:

* React + TypeScript Frontend
* Spring Boot Backend
* PostgreSQL Database
* H2 Test Database
* API Gateway
* Eureka Server
* Config Server
* User Service
* Reporting Service
* Redis
* Prometheus
* Grafana
* GitHub Actions CI/CD

General flow:

```text
Browser / React Frontend
        |
        v
API Gateway / Spring Boot API
        |
        v
Backend Services
        |
        v
Database
```

</details>

---

<details>
<summary><h2>🗺️ ER Diagram</h2></summary>

The diagram below documents the current entity relationships used in this project.

### Mermaid Plugin (IntelliJ)

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

### Notes

* `Order` is mapped to table name `orders` in code.
* `Payment` is a one-to-one relation with `Order`.
* `OrderItem` acts as the line-item bridge between `Order` and `MenuItem`.

</details>

---

<details>
<summary><h2>✨ Main Features</h2></summary>

### Users and Roles

* user creation;
* role management;
* login;
* role-based access control.

### Menu Management

* categories CRUD;
* menu items CRUD;
* product price management.

### Restaurant Tables

* table CRUD;
* assigning waiters to tables.

### Orders

* create orders;
* assign waiter and table;
* add menu items to an order;
* automatic total price calculation;
* order status management.

### Payments

* payment creation;
* CASH / CARD payment method;
* paid / unpaid status;
* automatic update of order status.

### AI Recommendations

The application includes an AI Runtime module that generates recommendations based on menu data.

Endpoint:

```http
GET /api/ai/recommendations
```

</details>

---

<details>
<summary><h2>💻 Frontend</h2></summary>

A React + TypeScript frontend is available in:

```text
frontend/
```

The frontend consumes backend endpoints under `/api/*`.

Technologies used:

* React
* TypeScript
* Vite
* React Router
* React Select
* custom CSS UI

Frontend URL:

```text
http://localhost:5173
```

</details>

---

<details>
<summary><h2>⚙️ Backend</h2></summary>

The backend is implemented using Spring Boot.

Technologies used:

* Java
* Spring Boot
* Spring Data JPA
* Spring Security
* Maven
* PostgreSQL
* H2 for tests

Backend URL:

```text
http://localhost:8080
```

</details>

---

<details>
<summary><h2>📡 API Documentation</h2></summary>

### Users

```http
GET    /api/users
GET    /api/users/{id}
POST   /api/users
PUT    /api/users/{id}
DELETE /api/users/{id}
```

### Categories

```http
GET    /api/categories
GET    /api/categories/{id}
POST   /api/categories
PUT    /api/categories/{id}
DELETE /api/categories/{id}
```

### Menu Items

```http
GET    /api/menu-items
GET    /api/menu-items/{id}
POST   /api/menu-items
PUT    /api/menu-items/{id}
DELETE /api/menu-items/{id}
```

### Restaurant Tables

```http
GET    /api/restaurant-tables
GET    /api/restaurant-tables/{id}
POST   /api/restaurant-tables
PUT    /api/restaurant-tables/{id}
DELETE /api/restaurant-tables/{id}
```

### Orders

```http
GET    /api/orders
GET    /api/orders/{id}
POST   /api/orders
PUT    /api/orders/{id}
DELETE /api/orders/{id}
```

### Order Items

```http
GET    /api/order-items
GET    /api/order-items/{id}
POST   /api/order-items
PUT    /api/order-items/{id}
DELETE /api/order-items/{id}
```

### Payments

```http
GET    /api/payments
GET    /api/payments/{id}
POST   /api/payments
PUT    /api/payments/{id}
DELETE /api/payments/{id}
```

### AI Recommendations

```http
GET /api/ai/recommendations
```

</details>

---

<details>
<summary><h2>🚀 Getting Started (Setup, Local Run & Dev Flow)</h2></summary>

### Status

* Backend running on port 8080
* Frontend running on port 5173
* CRUD operations implemented
* Role-based access implemented
* Testing implemented
* CI/CD configured
* Monitoring configured
* AI Recommendations implemented

### How to Start Locally

**Terminal 1 - Backend**

```powershell
cd C:\Users\Admin\Desktop\restaurantapp
.\mvnw.cmd spring-boot:run
```

**Terminal 2 - Frontend**

```powershell
cd C:\Users\Admin\Desktop\restaurantapp\frontend
npm install
npm run dev
```

**Browser**

Open:

```text
http://localhost:5173
```

### Local Development Flow

```text
┌─────────────────────────────────────────────────────────┐
│ Browser: http://localhost:5173                          │
│ React app with Vite dev server                          │
└──────────────────┬──────────────────────────────────────┘
                   │
        ┌──────────▼──────────┐
        │   Vite Proxy        │
        │   regex: ^/api      │
        │   ↓                 │
        │ http://localhost:8080
        │
┌──────────────────▼──────────────────────────────────────┐
│ Spring Boot API: http://localhost:8080                  │
│ GET /api/categories                                     │
│ POST /api/categories                                    │
│ PUT /api/categories/{id}                                │
│ DELETE /api/categories/{id}                             │
└─────────────────────────────────────────────────────────┘
        │
        ▼
   Database
```

</details>

---

<details>
<summary><h2>🔐 Environment Variables</h2></summary>

Environment-specific values are stored outside the source code whenever possible.

Examples:

* database URL;
* database username;
* database password;
* service ports;
* Redis configuration.

For tests, H2 is configured separately through the test profile.

The project supports environment-specific configurations.

Examples of configurable properties:

* database URL
* database username
* database password
* server ports
* Redis configuration
* Prometheus configuration
* Grafana configuration

For testing purposes, a dedicated H2 in-memory database configuration is used.

Example:

```properties
spring.jpa.hibernate.ddl-auto=create-drop
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
```

Separate Spring profiles are used for development and testing environments.

</details>

---

<details>
<summary><h2>🧪 Testing</h2></summary>

The project includes both unit tests and integration tests.

### Unit Tests

Technologies:

* JUnit 5
* Mockito

The service layer is tested with mocked repositories.

### Integration Tests

Technologies:

* Spring Boot Test
* MockMvc
* H2 Test Database

Integration scenarios:

* complete order creation flow;
* adding products to an order;
* deleting order items and recalculating totals;
* creating and updating payments;
* updating order status based on payments.

### Coverage

JaCoCo is used for code coverage.

Service layer coverage is above the required 70%.

Coverage report:

```text
target/site/jacoco/index.html
```

### Test Database Configuration

Tests run using the `test` profile and H2 in-memory database.

Example configuration:

```properties
spring.jpa.hibernate.ddl-auto=create-drop
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
```

</details>

---

<details>
<summary><h2>🔁 CI/CD Pipeline</h2></summary>

GitHub Actions is used for CI/CD.

The pipeline includes:

* backend build;
* automated test execution;
* frontend build;
* Docker Compose build;
* staging deployment simulation.

The workflow runs automatically on push and pull requests.

</details>

---

<details>
<summary><h2>🧩 Infrastructure & Microservices</h2></summary>

### Microservices Architecture

The optional microservices part was implemented using:

* Config Server
* Eureka Server
* API Gateway
* User Service
* Reporting Service
* Main Restaurant Service

**Config Server** — Used for centralized configuration.

**Eureka Server** — Used for service discovery.

**API Gateway** — Used for centralized routing, request filtering and gateway access.

**User Service** — Used for authentication and user-related responsibilities.

**Reporting Service** — Used for reporting and inter-service communication demonstrations.

### Service Discovery

Eureka is used as a service registry.

Services register automatically in Eureka and can be discovered by name.

Eureka Dashboard:

```text
http://localhost:8761
```

### API Gateway

API Gateway provides:

* centralized routing;
* request filtering;
* response filtering;
* integration with service discovery.

Gateway URL:

```text
http://localhost:8085
```

### Load Balancing

Spring Cloud LoadBalancer was used to demonstrate multiple instances of a service.

The application can run multiple instances of the same service and route requests between them through service discovery.

### Design Pattern — Strangler Fig

The project uses the Strangler Fig Pattern as a migration approach from a monolithic application to a microservice-based architecture.

The original Restaurant App was kept functional, while additional services were introduced gradually:

* User Service
* Reporting Service
* API Gateway
* Config Server
* Eureka Server

This allows the application to evolve gradually without rewriting the entire system at once.

Benefits:

* reduced migration risk;
* gradual extraction of responsibilities;
* independent service evolution;
* better scalability.

### NoSQL and Caching

Redis is used as a NoSQL / caching infrastructure component.

It is used in the project infrastructure for fast access and scalability-related features.

Benefits:

* faster access to frequently used data;
* reduced database load;
* improved scalability;
* support for infrastructure features such as rate limiting.

### Monitoring

Monitoring was implemented using:

* Spring Boot Actuator
* Prometheus
* Grafana

Prometheus URL:

```text
http://localhost:9090
```

Grafana URL:

```text
http://localhost:3000
```

Metrics monitored:

* JVM memory
* CPU usage
* HTTP requests
* application health

### Docker Deployment

The project can be started using Docker Compose.

```powershell
docker compose up -d
```

Dockerized / infrastructure services:

* PostgreSQL
* Redis
* Prometheus
* Grafana
* backend containers
* frontend container

### Deployment

Start all services:

```powershell
docker compose up -d
```

Stop all services:

```powershell
docker compose down
```

Available services:

| Service          | URL                   |
| ---------------- | --------------------- |
| Frontend         | http://localhost:5173 |
| Backend API      | http://localhost:8080 |
| API Gateway      | http://localhost:8085 |
| Eureka Dashboard | http://localhost:8761 |
| Prometheus       | http://localhost:9090 |
| Grafana          | http://localhost:3000 |

The Docker Compose configuration allows all components of the application to be started with a single command.

### Branch Strategy

Git was used during the entire development process.

Repository structure:

* master – main branch
* feature branches used during development and integration

GitHub Actions workflows are executed automatically on push and pull request events.

</details>

---

<details>
<summary><h2>🤖 AI Agents — Development & Runtime</h2></summary>

### GitHub Copilot for Pair Programming

GitHub Copilot was used during the development process as an AI-assisted programming tool.

It provided real-time code suggestions for backend and frontend implementation, helping reduce development time and improve productivity.

### Automated Code Review for Pull Requests

GitHub Copilot was also used for analyzing Pull Requests and suggesting improvements.

Examples:

* detecting duplicated code;
* suggesting method simplification;
* recommending best practices for Spring Boot and React;
* checking code consistency;
* identifying possible problems before merging.

### Automatic Documentation Generation

AI-based tools were used to generate and improve project documentation, including:

* microservices architecture explanation;
* testing documentation;
* CI/CD pipeline explanation;
* Docker and GitHub Actions configuration explanations.

### Benefits

* reduced development time;
* increased productivity;
* improved code quality;
* faster debugging;
* automated documentation support;
* support for code review and refactoring.

### AI Runtime — Recommendations

The application includes an AI Runtime feature through the AI Recommendations module.

Endpoint:

```http
GET /api/ai/recommendations
```

The module analyzes menu items and generates smart recommendations that can help restaurant staff promote specific products.

This feature demonstrates AI integration at runtime inside the application.

</details>

---

<details>
<summary><h2>📸 Screenshots</h2></summary>

### LOGIN PAGE
  <img width="1853" height="907" alt="image" src="https://github.com/user-attachments/assets/30d92f75-33ef-4df9-a7bc-fdc931b45ef8" />
  
### REGISTER PAGE
<img width="1805" height="863" alt="image" src="https://github.com/user-attachments/assets/ade035bb-fc00-4a3d-9abe-dd4ed2c820b7" />

### DASHBOARD VIEW
<img width="1919" height="896" alt="image" src="https://github.com/user-attachments/assets/c7ad90f7-1c62-465a-a557-417b50081017" />

### PAYMENT PAGE
<img width="1887" height="891" alt="image" src="https://github.com/user-attachments/assets/4636dee4-da2e-4733-a76e-40a87839098a" />

### AI RECOMMENDATION
<img width="1893" height="898" alt="image" src="https://github.com/user-attachments/assets/5f457ca8-ae69-4f89-8737-95e0e7843306" />

### GRAFANA
<img width="1289" height="768" alt="image" src="https://github.com/user-attachments/assets/7a340a9c-2631-4d34-ad8b-59cd52da4a77" />

### JACOCO
<img width="1382" height="301" alt="image" src="https://github.com/user-attachments/assets/2c3baa79-195e-445f-b767-22d63962fc7c" />

</details>

---

<details>
<summary><h2>✅ Implemented Requirements</h2></summary>

### Mandatory Requirements

* Model de date
* CRUD operations
* Multi-environment configuration
* Testing
* Views and validation
* Logging
* Pagination and sorting
* Spring Security

### Optional Requirements

* Centralized configuration
* Service discovery
* Load balancing
* API Gateway
* Monitoring
* Resilience
* Design patterns

### Bonus

* AI in development
* AI runtime integration
* CI/CD pipeline
* Docker deployment

</details>

---

<details>
<summary><h2>👥 Team Members and Contributions</h2></summary>

### 405 - BDTS
### Cimpeanu Ana-Maria
### Iova Nicoleta-Carmen
### Tismanaru Artemis-Constantina

Contributions:

* backend development;
* frontend development;
* database design;
* CRUD implementation;
* security and role-based access;
* testing;
* microservices setup;
* monitoring setup;
* CI/CD configuration;
* AI features;
* documentation.

</details>

---

# Conclusion

Restaurant App is a full-stack restaurant management system built with enterprise technologies.

The project includes backend, frontend, database persistence, role-based access, tests, monitoring, microservice infrastructure, CI/CD automation, AI-assisted development and runtime AI recommendations.
