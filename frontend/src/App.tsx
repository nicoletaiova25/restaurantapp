import { useEffect, useState } from 'react';
import { NavLink, Navigate, Route, Routes, useLocation, useParams } from 'react-router-dom';
import { ApiError, api } from './api';
import { entityConfigByKey, entityConfigs, validateEntityForm, type EntityConfig, type EntityField } from './entities';
import type { EntityName } from './types';
import LoginPage from './LoginPage';
import RegisterPage from './RegisterPage';
import Select from 'react-select';

type FormValues = Record<string, string | boolean>;
type Row = Record<string, any>;

type RelationOption = {
  id: number;
  label: string;
};

type LoggedUser = {
  id: number;
  username: string;
  role: string;
};

const getLoggedUser = (): LoggedUser | null => {
  const rawUser = localStorage.getItem('loggedUser') ?? sessionStorage.getItem('loggedUser');

  if (!rawUser) return null;

  try {
    return JSON.parse(rawUser);
  } catch {
    localStorage.removeItem('loggedUser');
    sessionStorage.removeItem('loggedUser');
    return null;
  }
};

const canAccessEntity = (entityKey: string) => {
  const user = getLoggedUser();
  if (!user) return false;

  if (user.role === 'ADMIN' || user.role === 'MANAGER') return true;

  if (user.role === 'WAITER') {
    return ['orders', 'order-items', 'payments'].includes(entityKey);
  }

  if (user.role === 'BARTENDER') {
    return ['orders', 'order-items'].includes(entityKey);
  }

  return false;
};

const getFirstEntityPath = () => {
  const user = getLoggedUser();

  if (!user) return '/login';

  if (user.role === 'WAITER') return '/entities/orders';
  if (user.role === 'BARTENDER') return '/entities/orders';

  const firstAllowed = entityConfigs.find((entity) => canAccessEntity(entity.key));
  return firstAllowed ? `/entities/${firstAllowed.key}` : '/404';
};

const filterRowsByRole = (entityKey: string | undefined, rows: Row[]) => {
  const user = getLoggedUser();

  if (!user) return [];

  if (user.role === 'ADMIN' || user.role === 'MANAGER') {
    return rows;
  }

  if (user.role === 'WAITER') {
    if (entityKey === 'orders') {
      return rows.filter((order) => order.waiter?.id === user.id);
    }

    if (entityKey === 'order-items') {
      return rows.filter((item) => item.order?.waiter?.id === user.id);
    }

    if (entityKey === 'payments') {
      return rows.filter((payment) => payment.order?.waiter?.id === user.id);
    }
  }

  if (user.role === 'BARTENDER') {
    if (entityKey === 'orders' || entityKey === 'order-items') {
      return rows;
    }
  }

  return [];
};

const createInitialValues = (entity: EntityConfig): FormValues => {
  const values: FormValues = {};

  entity.fields.forEach((field) => {
    values[field.key] = field.type === 'boolean' ? false : '';
  });

  return values;
};

const buildPayload = (entity: EntityConfig, values: FormValues): Record<string, unknown> => {
  const payload: Record<string, unknown> = {};

  entity.fields.forEach((field) => {
    const value = values[field.key];

    if (field.type === 'boolean') {
      payload[field.key] = Boolean(value);
      return;
    }

    if (field.type === 'number') {
      payload[field.key] = Number(value);
      return;
    }

    if (field.type === 'relationId') {
      payload[field.relationName ?? field.key] = { id: Number(value) };
      return;
    }

    payload[field.key] = String(value).trim();
  });

  return payload;
};

const getNestedValue = (row: Row, field: EntityField): unknown => {
  if (field.type === 'relationId') {
    const relationName = field.relationName ?? field.key;
    const relation = row[relationName];

    if (relation && typeof relation === 'object') {
      if (field.labelField && relation[field.labelField] !== undefined) return relation[field.labelField];
      if (relation.name !== undefined) return relation.name;
      if (relation.username !== undefined) return relation.username;
      if (relation.tableNumber !== undefined) return relation.tableNumber;
      if (relation.id !== undefined) return relation.id;
    }

    return row[field.key] ?? '';
  }

  return row[field.key] ?? '';
};

const buildRelationLabel = (item: Row, field: EntityField): string => {
  const labelField = field.labelField;

  if (field.relationName === 'order') {
    const parts = [`Order #${item.id}`];

    if (item.table?.tableNumber !== undefined) {
      parts.push(`Table ${item.table.tableNumber}`);
    }

    if (item.waiter?.username !== undefined) {
      parts.push(`Waiter ${item.waiter.username}`);
    }

    if (item.totalPrice !== undefined) {
      parts.push(`Total ${Number(item.totalPrice).toFixed(2)}`);
    }

    return parts.join(' - ');
  }

  if (labelField && item[labelField] !== undefined) {
    return String(item[labelField]);
  }

  return String(item.name ?? item.username ?? item.tableNumber ?? item.id);
};

const getExtraColumns = (entityKey?: string): string[] => {
  if (entityKey === 'order-items') return ['Unit Price', 'Item Total'];
  if (entityKey === 'orders') return ['Total Price'];
  if (entityKey === 'payments') return ['Order Total'];

  return [];
};

const getExtraColumnValue = (entityKey: string | undefined, row: Row, column: string): string => {
  if (entityKey === 'order-items') {
    const price = Number(row.menuItem?.price ?? 0);
    const quantity = Number(row.quantity ?? 0);

    if (column === 'Unit Price') return price.toFixed(2);
    if (column === 'Item Total') return (price * quantity).toFixed(2);
  }

  if (entityKey === 'orders' && column === 'Total Price') {
    return Number(row.totalPrice ?? 0).toFixed(2);
  }

  if (entityKey === 'payments' && column === 'Order Total') {
    return Number(row.order?.totalPrice ?? 0).toFixed(2);
  }

  return '';
};

function EntityCrudPage() {
  const { entityKey } = useParams();
  const entity = entityConfigByKey[entityKey as EntityName];

  const [formValues, setFormValues] = useState<FormValues>(() => (entity ? createInitialValues(entity) : {}));
  const [formErrors, setFormErrors] = useState<Record<string, string>>({});
  const [editingId, setEditingId] = useState<number | null>(null);
  const [rows, setRows] = useState<Row[]>([]);
  const [status, setStatus] = useState('Ready');
  const [error, setError] = useState('');
  const [isBusy, setIsBusy] = useState(false);
  const [relationOptions, setRelationOptions] = useState<Record<string, RelationOption[]>>({});

  useEffect(() => {
    if (!entity) return;

    setFormValues(createInitialValues(entity));
    setFormErrors({});
    setEditingId(null);
    setRows([]);
    setStatus('Ready');
    setError('');
    setRelationOptions({});
  }, [entityKey, entity]);

  const withApi = async (action: () => Promise<void>, successMessage?: string) => {
    setIsBusy(true);
    setError('');

    try {
      await action();

      if (successMessage) {
        setStatus('✅ ' + successMessage);
      }
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Unexpected error';

      if (err instanceof ApiError && err.isServerError) {
        setError('Nu se poate face operația. Posibil elementul este folosit în altă parte.');
      } else {
        setError(msg);
      }
    } finally {
      setIsBusy(false);
    }
  };

  const refreshAll = async () => {
    if (!entity) return;

    const data = await api.getAll<any>(entity.endpoint);
    const pageData = data as any;

    const rawRows = Array.isArray(pageData)
      ? pageData
      : Array.isArray(pageData?.content)
        ? pageData.content
        : [];

    const filteredRows = filterRowsByRole(entityKey, rawRows);

    setRows(filteredRows);
    setStatus(`Loaded ${filteredRows.length} ${entity.label.toLowerCase()}.`);
  };

  useEffect(() => {
    refreshAll();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [entityKey]);

  useEffect(() => {
    if (!entity) return;

    const loadOptions = async () => {
      const relationFields = entity.fields.filter((field) => field.type === 'relationId' && field.endpoint);

      for (const field of relationFields) {
        try {
          const key = field.relationName ?? field.key;
          const data = await api.getAll<any>(field.endpoint as string);
          const pageData = data as any;

          const optionData = Array.isArray(pageData)
            ? pageData
            : Array.isArray(pageData?.content)
              ? pageData.content
              : [];

          const loggedUser = getLoggedUser();
          let filteredData = optionData;

          if (loggedUser?.role === 'WAITER') {
            if (field.relationName === 'table' || field.key === 'table') {
              filteredData = optionData.filter((table: Row) => table.waiter?.id === loggedUser.id);
            }

            if (field.relationName === 'order' || field.key === 'order') {
              filteredData = optionData.filter((order: Row) => order.waiter?.id === loggedUser.id);
            }
          }

          const opts = filteredData.map((item: Row) => ({
            id: Number(item.id),
            label: buildRelationLabel(item, field),
          }));

          setRelationOptions((prev) => ({
            ...prev,
            [key]: opts,
          }));
        } catch {
          setError('Could not load relation options.');
        }
      }
    };

    loadOptions();
  }, [entityKey, entity]);

  if (!entity) {
    return <Navigate to="/404" replace />;
  }

  if (!canAccessEntity(entityKey ?? '')) {
    return <Navigate to={getFirstEntityPath()} replace />;
  }

  const onChange = (key: string, value: string | boolean) => {
    setFormValues((prev) => ({
      ...prev,
      [key]: value,
    }));
  };

  const populateFormFromRow = (row: Row) => {
    const values: FormValues = {};

    entity.fields.forEach((field) => {
      if (field.type === 'boolean') {
        values[field.key] = Boolean(row[field.key]);
        return;
      }

      if (field.type === 'relationId') {
        const relationName = field.relationName ?? field.key;
        const relation = row[relationName];

        if (relation && typeof relation === 'object' && relation.id !== undefined) {
          values[field.key] = String(relation.id);
        } else if (row[field.key] !== undefined) {
          values[field.key] = String(row[field.key]);
        } else {
          values[field.key] = '';
        }

        return;
      }

      values[field.key] = row[field.key] !== undefined ? String(row[field.key]) : '';
    });

    setFormValues(values);
  };

  const startEdit = (row: Row) => {
    if (!row || !row.id) return;

    setEditingId(Number(row.id));
    populateFormFromRow(row);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const clearForm = () => {
    setFormValues(createInitialValues(entity));
    setEditingId(null);
    setFormErrors({});
    setError('');
    setStatus('Ready');
  };

  const runCreateOrUpdate = async (mode: 'create' | 'update') => {
    const validation = validateEntityForm(entity, formValues);

    if (mode === 'update' && (!editingId || editingId <= 0)) {
      validation._edit = 'Selecteaza un element din tabel pentru a edita.';
    }

    setFormErrors(validation);

    if (Object.keys(validation).length > 0) {
      setError('Date invalide. Corecteaza campurile marcate.');
      return;
    }

    const payload = buildPayload(entity, formValues);

    await withApi(async () => {
      if (mode === 'create') {
        await api.create<Row>(entity.endpoint, payload);
      } else {
        await api.update<Row>(entity.endpoint, editingId as number, payload);
      }

      await refreshAll();
      clearForm();
    }, mode === 'create' ? 'Created successfully.' : 'Updated successfully.');
  };

  const runDelete = async (id?: number) => {
    const targetId = id ?? editingId;

    if (!targetId || targetId <= 0) {
      setError('Selecteaza un element valid pentru stergere.');
      return;
    }

    const confirmed = window.confirm(
      `Sigur vrei sa stergi acest element? ID: ${targetId}`
    );

    if (!confirmed) return;

    await withApi(async () => {
      await api.remove(entity.endpoint, targetId);
      await refreshAll();

      if (editingId === targetId) {
        clearForm();
      }
    }, 'Deleted successfully.');
  };

  return (
    <main className="content">
      <div className="panel header-panel">
        <h2 className="brand">🥐 La Petite Table — {entity.label}</h2>
        <p className="lead">
          {entityKey === 'orders'
            ? 'Creeaza si gestioneaza comenzile.'
            : entityKey === 'order-items'
              ? 'Adauga produse in comenzile deschise.'
              : entityKey === 'payments'
                ? 'Gestioneaza platile comenzilor.'
                : `Manage ${entity.label.toLowerCase()}.`}
        </p>
      </div>

      <div className="panel">
        <h3>{editingId ? 'Editing item #' + editingId : 'Create new'}</h3>

        <div className="form-grid">
          {entity.fields.map((field) => (
            <label key={field.key} className={`field ${formErrors[field.key] ? 'has-error' : ''}`}>
              <span>
                {field.label}
                {field.required ? ' *' : ''}
              </span>

                {field.type === 'boolean' ? (
                  <label className="toggle-switch">
                    <input
                      type="checkbox"
                      checked={Boolean(formValues[field.key])}
                      onChange={(event) => onChange(field.key, event.target.checked)}
                    />
                    <span className="toggle-slider"></span>
                  </label>
                ) : field.type === 'relationId' ? (
                <Select
                  className="fancy-select"
                  classNamePrefix="fancy-select"
                  placeholder="Selecteaza..."
                  isClearable
                  options={(relationOptions[field.relationName ?? field.key] ?? []).map((option) => ({
                    value: String(option.id),
                    label: option.label,
                  }))}
                  value={
                    (relationOptions[field.relationName ?? field.key] ?? [])
                      .map((option) => ({
                        value: String(option.id),
                        label: option.label,
                      }))
                      .find((option) => option.value === String(formValues[field.key] ?? '')) ?? null
                  }
                  onChange={(selected) => onChange(field.key, selected ? selected.value : '')}
                />
              ) : field.type === 'select' ? (
                <Select
                  className="fancy-select"
                  classNamePrefix="fancy-select"
                  placeholder="Selecteaza..."
                  isClearable
                  options={(field.options ?? []).map((option) => ({
                    value: option.value,
                    label: option.label,
                  }))}
                  value={
                    (field.options ?? [])
                      .map((option) => ({
                        value: option.value,
                        label: option.label,
                      }))
                      .find((option) => option.value === String(formValues[field.key] ?? '')) ?? null
                  }
                  onChange={(selected) => onChange(field.key, selected ? selected.value : '')}
                />
              ) : (
                <input
                  aria-invalid={Boolean(formErrors[field.key])}
                  type={field.type === 'text' ? 'text' : 'number'}
                  step={field.integer ? '1' : 'any'}
                  value={String(formValues[field.key] ?? '')}
                  onChange={(event) => onChange(field.key, event.target.value)}
                  placeholder={field.required ? 'required' : ''}
                />
              )}

              {formErrors[field.key] ? <small className="error">⚠️ {formErrors[field.key]}</small> : null}
            </label>
          ))}
        </div>

        <div className="actions">
          <button type="button" onClick={() => runCreateOrUpdate('create')} disabled={isBusy}>
            ➕ Create
          </button>

          <button type="button" onClick={() => runCreateOrUpdate('update')} disabled={isBusy || !editingId}>
            ✏️ Save
          </button>

          <button type="button" onClick={() => runDelete()} disabled={isBusy || !editingId}>
            🗑️ Delete
          </button>

          <button type="button" onClick={clearForm} disabled={isBusy}>
            ✖️ Clear
          </button>

          <button type="button" onClick={refreshAll} disabled={isBusy}>
            🔄 Refresh
          </button>
        </div>
      </div>

      <p className="status">{isBusy ? 'Working...' : status}</p>
      {error ? <p className="error">⚠️ {error}</p> : null}

      <div className="panel">
        <h3>Items</h3>

        <div className="table-wrap">
          <table className="result-table">
            <thead>
              <tr>
                <th>ID</th>

                {entity.fields.map((field) => (
                  <th key={field.key}>{field.label}</th>
                ))}

                {getExtraColumns(entityKey).map((column) => (
                  <th key={column}>{column}</th>
                ))}

                <th>Actions</th>
              </tr>
            </thead>

            <tbody>
              {rows.map((row) => (
                <tr key={row.id} className={editingId === row.id ? 'row-active' : ''}>
                  <td>{row.id}</td>

                  {entity.fields.map((field) => (
                    <td key={field.key}>{String(getNestedValue(row, field))}</td>
                  ))}

                  {getExtraColumns(entityKey).map((column) => (
                    <td key={column}>{getExtraColumnValue(entityKey, row, column)}</td>
                  ))}

                  <td>
                    <button type="button" onClick={() => startEdit(row)}>
                      Edit
                    </button>

                    <button type="button" onClick={() => runDelete(Number(row.id))}>
                      Delete
                    </button>
                  </td>
                </tr>
              ))}

              {rows.length === 0 ? (
                <tr>
                  <td colSpan={entity.fields.length + getExtraColumns(entityKey).length + 2}>
                    No items found.
                  </td>
                </tr>
              ) : null}
            </tbody>
          </table>
        </div>
      </div>
    </main>
  );
}

function NotFoundPage() {
  return (
    <main className="content error-page">
      <div className="panel center">
        <h2>404 — Pagina indisponibila</h2>
        <p>Nu ai acces la aceasta zona sau pagina nu exista.</p>
        <p>
          <a href="/">Mergi la pagina principala</a>
        </p>
      </div>
    </main>
  );
}

function ServerErrorPage() {
  const location = useLocation();
  const message = (location.state as { message?: string } | null)?.message;

  return (
    <main className="content error-page">
      <div className="panel center">
        <h2>500 — Eroare server</h2>
        <p>{message ?? 'A aparut o eroare pe server.'}</p>
        <p>
          <a href="/">Mergi la pagina principala</a>
        </p>
      </div>
    </main>
  );
}

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const user = getLoggedUser();

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  return children;
}

function App() {
  const location = useLocation();
  const isAuthPage = location.pathname === '/login' || location.pathname === '/register';

  const logout = () => {
    localStorage.removeItem('loggedUser');
    sessionStorage.removeItem('loggedUser');
    window.location.href = '/login';
  };

  return (
    <div className="layout">
      {!isAuthPage ? (
        <aside className="sidebar">
          <h1 className="brand">🥐 La Petite Table</h1>
          <p className="sidebar-sub">Restaurant panel</p>

          {entityConfigs
            .filter((entity) => canAccessEntity(entity.key))
            .map((entity) => (
              <NavLink
                key={entity.key}
                to={`/entities/${entity.key}`}
                className={({ isActive }) => (isActive ? 'tab active' : 'tab')}
              >
                {entity.label}
              </NavLink>
            ))}

          <button type="button" className="tab" onClick={logout}>
            Logout
          </button>

          <footer className="sidebar-footer">La Petite Table</footer>
        </aside>
      ) : null}

      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />

        <Route
          path="/"
          element={
            <ProtectedRoute>
              <Navigate to={getFirstEntityPath()} replace />
            </ProtectedRoute>
          }
        />

        <Route
          path="/entities/:entityKey"
          element={
            <ProtectedRoute>
              <EntityCrudPage />
            </ProtectedRoute>
          }
        />

        <Route path="/error/500" element={<ServerErrorPage />} />
        <Route path="/404" element={<NotFoundPage />} />
        <Route path="*" element={<Navigate to="/404" replace />} />
      </Routes>

      {!isAuthPage ? <footer className="global-footer">© La Petite Table</footer> : null}
    </div>
  );
}

export default App;