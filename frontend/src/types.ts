export type Id = number;

export type Category = {
  id?: Id;
  name: string;
};

export type User = {
  id?: Id;
  username: string;
  password: string;
  role: string;
};

export type RestaurantTable = {
  id?: Id;
  tableNumber: number;
  seats: number;
  waiter: User;
};

export type MenuItem = {
  id?: Id;
  name: string;
  price: number;
  category: Category;
};

export type Order = {
  id?: Id;
  table: RestaurantTable;
  waiter: User;
  status: string;
  totalPrice: number;
};

export type OrderItem = {
  id?: Id;
  order: Order;
  menuItem: MenuItem;
  quantity: number;
};

export type Payment = {
  id?: Id;
  order: Order;
  method: string;
  paid: boolean;
};

export type EntityName =
  | 'users'
  | 'categories'
  | 'restaurant-tables'
  | 'menu-items'
  | 'orders'
  | 'order-items'
  | 'payments';

