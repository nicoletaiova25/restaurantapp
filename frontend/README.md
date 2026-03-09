# React Frontend (Vite + TypeScript)

This frontend talks to the Spring Boot API and gives a CRUD UI for all entities.

## Quick Start

### 1. Backend (Spring Boot)

```powershell
cd C:\Users\Admin\Desktop\restaurantapp
.\mvnw.cmd spring-boot:run
```

Backend will start on **http://localhost:8080**

### 2. Frontend (Vite dev server)

```powershell
cd C:\Users\Admin\Desktop\restaurantapp\frontend
npm install
npm run dev
```

Frontend will start on **http://localhost:5173**

## Testing

### Verify Backend is Up

```powershell
Invoke-WebRequest -Uri "http://localhost:8080/api/categories" -UseBasicParsing
```

Should return JSON like: `[{"id":1,"name":"Desserts"}]`

### Verify Vite Proxy Works

```powershell
Invoke-WebRequest -Uri "http://localhost:5173/api/categories" -UseBasicParsing
```

Should also return JSON (proxied from backend)

### Create a Category (test POST)

```powershell
Invoke-WebRequest -Uri "http://localhost:5173/api/categories" `
  -Method POST `
  -Headers @{'Content-Type'='application/json'} `
  -Body '{"name":"Test"}' `
  -UseBasicParsing
```

## Common Issues

### "Unexpected token '<'" or "HTML Response"

- Make sure backend is running on port 8080
- Check that Vite proxy is configured in `vite.config.ts`
- Clear browser cache: Ctrl+Shift+Delete → Empty Cache and Hard Refresh (Ctrl+F5)
- Check `.env.development` has `VITE_API_BASE_URL=/api`

### "Nu ma pot conecta la backend"

- Backend is not running. Start it with `.\mvnw.cmd spring-boot:run`
- Check port 8080 is listening: `netstat -ano | findstr "8080"`

### Categories show "0" after create

- Backend created it but page didn't refresh
- Click "Read All" button to manually refresh
- Check browser console for JavaScript errors (F12)

## Forms & Validation

- Each entity has its own form with fields validated client-side
- Errors show inline under each field
- Server-side @Valid errors from backend show in global error message
- 404 and 500 errors have dedicated pages

## Entity Routes

- `/entities/users` - User management
- `/entities/categories` - Food categories
- `/entities/restaurant-tables` - Restaurant tables
- `/entities/menu-items` - Menu items
- `/entities/orders` - Orders
- `/entities/order-items` - Order items (line items)
- `/entities/payments` - Payments
