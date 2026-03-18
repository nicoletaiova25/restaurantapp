import type { EntityName } from './types';

export type FieldType = 'text' | 'number' | 'boolean' | 'relationId';

export type EntityField = {
  key: string;
  label: string;
  type: FieldType;
  required?: boolean;
  min?: number;
  integer?: boolean;
  relationName?: string;
};

export type EntityConfig = {
  key: EntityName;
  label: string;
  endpoint: string;
  fields: EntityField[];
};

export const entityConfigs: EntityConfig[] = [
  {
    key: 'users',
    label: 'Users',
    endpoint: '/users',
    fields: [
      { key: 'username', label: 'Username', type: 'text', required: true },
      { key: 'password', label: 'Password', type: 'text', required: true },
      { key: 'role', label: 'Role', type: 'text', required: true },
    ],
  },
  {
    key: 'categories',
    label: 'Categories',
    endpoint: '/categories',
    fields: [{ key: 'name', label: 'Name', type: 'text', required: true }],
  },
  {
    key: 'restaurant-tables',
    label: 'Restaurant Tables',
    endpoint: '/restaurant-tables',
    fields: [
      { key: 'tableNumber', label: 'Table Number', type: 'number', required: true, min: 1, integer: true },
      { key: 'seats', label: 'Seats', type: 'number', required: true, min: 1, integer: true },
      { key: 'waiterId', label: 'Waiter ID', type: 'relationId', required: true, relationName: 'waiter' },
    ],
  },
  {
    key: 'menu-items',
    label: 'Menu Items',
    endpoint: '/menu-items',
    fields: [
      { key: 'name', label: 'Name', type: 'text', required: true },
      { key: 'price', label: 'Price', type: 'number', required: true, min: 0.01 },
      { key: 'categoryId', label: 'Category ID', type: 'relationId', required: true, relationName: 'category' },
    ],
  },
  {
    key: 'orders',
    label: 'Orders',
    endpoint: '/orders',
    fields: [
      { key: 'tableId', label: 'Table ID', type: 'relationId', required: true, relationName: 'table' },
      { key: 'waiterId', label: 'Waiter ID', type: 'relationId', required: true, relationName: 'waiter' },
      { key: 'status', label: 'Status', type: 'text', required: true },
      { key: 'totalPrice', label: 'Total Price', type: 'number', required: true, min: 0 },
    ],
  },
  {
    key: 'order-items',
    label: 'Order Items',
    endpoint: '/order-items',
    fields: [
      { key: 'orderId', label: 'Order ID', type: 'relationId', required: true, relationName: 'order' },
      { key: 'menuItemId', label: 'Menu Item ID', type: 'relationId', required: true, relationName: 'menuItem' },
      { key: 'quantity', label: 'Quantity', type: 'number', required: true, min: 1, integer: true },
    ],
  },
  {
    key: 'payments',
    label: 'Payments',
    endpoint: '/payments',
    fields: [
      { key: 'orderId', label: 'Order ID', type: 'relationId', required: true, relationName: 'order' },
      { key: 'method', label: 'Method', type: 'text', required: true },
      { key: 'paid', label: 'Paid', type: 'boolean', required: true },
    ],
  },
];

export const entityConfigByKey = entityConfigs.reduce<Record<EntityName, EntityConfig>>((acc, entity) => {
  acc[entity.key] = entity;
  return acc;
}, {} as Record<EntityName, EntityConfig>);

export type ValidationErrors = Record<string, string>;

export const validateEntityForm = (entity: EntityConfig, values: Record<string, string | boolean>): ValidationErrors => {
  const errors: ValidationErrors = {};

  entity.fields.forEach((field) => {
    const raw = values[field.key];
    const value = typeof raw === 'string' ? raw.trim() : raw;

    if (field.type === 'boolean') {
      return;
    }

    if (field.required && (value === '' || value === undefined)) {
      errors[field.key] = `${field.label} este obligatoriu.`;
      return;
    }

    if ((field.type === 'number' || field.type === 'relationId') && value !== '' && value !== undefined) {
      const num = Number(value);
      if (!Number.isFinite(num)) {
        errors[field.key] = `${field.label} trebuie sa fie numar valid.`;
        return;
      }
      if (field.integer && !Number.isInteger(num)) {
        errors[field.key] = `${field.label} trebuie sa fie numar intreg.`;
        return;
      }
      if (field.min !== undefined && num < field.min) {
        errors[field.key] = `${field.label} trebuie sa fie >= ${field.min}.`;
      }
    }
  });

  return errors;
};
