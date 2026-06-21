import type { EntityName } from './types';

export type FieldType = 'text' | 'number' | 'boolean' | 'relationId' | 'select';

export type SelectOption = {
  value: string;
  label: string;
};

export type EntityField = {
  key: string;
  label: string;
  type: FieldType;
  required?: boolean;
  min?: number;
  integer?: boolean;

  // Used for relation fields. Example: key = waiter, relationName = waiter.
  relationName?: string;

  // Exact backend endpoint used to load dropdown values.
  // Example: /users, /categories, /restaurant-tables.
  endpoint?: string;

  // Field displayed in dropdown instead of id.
  // Example: username, name, tableNumber.
  labelField?: string;

  // Static dropdown values, used for role, payment method, status etc.
  options?: SelectOption[];
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
      {
        key: 'role',
        label: 'Role',
        type: 'select',
        required: true,
        options: [
          { value: 'MANAGER', label: 'Manager' },
          { value: 'WAITER', label: 'Ospatar' },
          { value: 'BARTENDER', label: 'Barman' },
        ],
      },
    ],
  },
  {
    key: 'categories',
    label: 'Categories',
    endpoint: '/categories',
    fields: [
      { key: 'name', label: 'Name', type: 'text', required: true },
    ],
  },
  {
    key: 'restaurant-tables',
    label: 'Restaurant Tables',
    endpoint: '/restaurant-tables',
    fields: [
      { key: 'tableNumber', label: 'Table Number', type: 'number', required: true, min: 1, integer: true },
      { key: 'seats', label: 'Seats', type: 'number', required: true, min: 1, integer: true },
      {
        key: 'waiter',
        label: 'Waiter Username',
        type: 'relationId',
        required: true,
        relationName: 'waiter',
        endpoint: '/users',
        labelField: 'username',
      },
    ],
  },
  {
    key: 'menu-items',
    label: 'Menu Items',
    endpoint: '/menu-items',
    fields: [
      { key: 'name', label: 'Name', type: 'text', required: true },
      { key: 'price', label: 'Price', type: 'number', required: true, min: 0.01 },
      {
        key: 'category',
        label: 'Category Name',
        type: 'relationId',
        required: true,
        relationName: 'category',
        endpoint: '/categories',
        labelField: 'name',
      },
    ],
  },
  {
    key: 'orders',
    label: 'Orders',
    endpoint: '/orders',
    fields: [
      {
        key: 'tableId',
        label: 'Table Number',
        type: 'relationId',
        required: true,
        relationName: 'table',
        endpoint: '/restaurant-tables',
        labelField: 'tableNumber'
      },
      {
        key: 'waiterId',
        label: 'Waiter Username',
        type: 'relationId',
        required: true,
        relationName: 'waiter',
        endpoint: '/users',
        labelField: 'username'
      }
    ],
  },
  {
    key: 'order-items',
    label: 'Order Items',
    endpoint: '/order-items',
    fields: [
      {
        key: 'order',
        label: 'Order',
        type: 'relationId',
        required: true,
        relationName: 'order',
        endpoint: '/orders',
        labelField: 'id',
      },
      {
        key: 'menuItem',
        label: 'Menu Item Name',
        type: 'relationId',
        required: true,
        relationName: 'menuItem',
        endpoint: '/menu-items',
        labelField: 'name',
      },
      { key: 'quantity', label: 'Quantity', type: 'number', required: true, min: 1, integer: true },
    ],
  },
  {
    key: 'payments',
    label: 'Payments',
    endpoint: '/payments',
    fields: [
      {
        key: 'order',
        label: 'Order',
        type: 'relationId',
        required: true,
        relationName: 'order',
        endpoint: '/orders',
        labelField: 'id',
      },
      {
        key: 'method',
        label: 'Method',
        type: 'select',
        required: true,
        options: [
          { value: 'CASH', label: 'Cash' },
          { value: 'CARD', label: 'Card' },
        ],
      },
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

    if (field.type === 'select' && value !== '' && value !== undefined) {
      const allowedValues = field.options?.map((option) => option.value) ?? [];
      if (allowedValues.length > 0 && !allowedValues.includes(String(value))) {
        errors[field.key] = `${field.label} trebuie ales din lista.`;
      }
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
