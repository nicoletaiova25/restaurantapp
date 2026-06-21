import { useEffect, useMemo, useState } from 'react';
import { NavLink, Navigate, Route, Routes, useLocation, useNavigate, useParams } from 'react-router-dom';
import { ApiError, api } from './api';
import { entityConfigByKey, entityConfigs, validateEntityForm, type EntityConfig, type EntityField } from './entities';
import type { EntityName } from './types';
import LoginPage from './LoginPage';
import RegisterPage from './RegisterPage';


type FormValues = Record<string, string | boolean>;
type Row = Record<string, any>;

type RelationOption = {
  id: number;
  label: string;
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
      if (field.labelField && relation[field.labelField] !== undefined) {
        return relation[field.labelField];
      }

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
    const tableNumber = item.table?.tableNumber;
    const waiterUsername = item.waiter?.username;
    const totalPrice = item.totalPrice;

    const parts = [`Order #${item.id}`];

    if (tableNumber !== undefined) {
      parts.push(`Table ${tableNumber}`);
    }

    if (waiterUsername !== undefined) {
      parts.push(`Waiter ${waiterUsername}`);
    }

    if (totalPrice !== undefined) {
      parts.push(`Total ${Number(totalPrice).toFixed(2)}`);
    }

    return parts.join(' - ');
  }

  if (labelField && item[labelField] !== undefined) {
    return String(item[labelField]);
  }

  return String(item.name ?? item.username ?? item.tableNumber ?? item.id);
};

const getExtraColumns = (entityKey?: string): string[] => {
  if (entityKey === 'order-items') {
    return ['Unit Price', 'Item Total'];
  }

  if (entityKey === 'orders') {
    return ['Total Price'];
  }

  if (entityKey === 'payments') {
    return ['Order Total'];
  }

  return [];
};

const getExtraColumnValue = (entityKey: string | undefined, row: Row, column: string): string => {
  if (entityKey === 'order-items') {
    const price = Number(row.menuItem?.price ?? 0);
    const quantity = Number(row.quantity ?? 0);

    if (column === 'Unit Price') {
      return price.toFixed(2);
    }

    if (column === 'Item Total') {
      return (price * quantity).toFixed(2);
    }
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
  const navigate = useNavigate();
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

  const setServerErrorPage = (apiError: ApiError) => {
    if (apiError.isServerError) {
      navigate('/error/500', { state: { message: apiError.message } });
    }
  };

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
      setError(msg);

      if (err instanceof ApiError) {
        setServerErrorPage(err);
      }
    } finally {
      setIsBusy(false);
    }
  };

  const refreshAll = async () => {
    if (!entity) return;

  const data = await api.getAll<any>(entity.endpoint);

  const pageData = data as any;

  const rowsData = Array.isArray(pageData)
    ? pageData
    : Array.isArray(pageData?.content)
      ? pageData.content
      : [];

  setRows(rowsData);
  setStatus(`Loaded ${rowsData.length} ${entity.label.toLowerCase()}.`);
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
         // const data = await api.getAll<Row>(field.endpoint as string);
          const key = field.relationName ?? field.key;

         const data = await api.getAll<any>(field.endpoint as string);

         const pageData = data as any;

         const optionData = Array.isArray(pageData)
           ? pageData
           : Array.isArray(pageData?.content)
             ? pageData.content
             : [];

         const opts = optionData.map((item: Row) => ({
           id: Number(item.id),
           label: buildRelationLabel(item, field),
         }));

          setRelationOptions((prev) => ({
            ...prev,
            [key]: opts,
          }));
        } catch (err) {
          const msg = err instanceof Error ? err.message : 'Could not load relation options';
          setError(msg);
        }
      }
    };

    loadOptions();
  }, [entityKey, entity]);

  if (!entity) {
    return <Navigate to="/404" replace />;
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

    if (mode === 'update') {
      if (!editingId || editingId <= 0) {
        validation._edit = 'Selecteaza un element din tabel pentru a edita.';
      }
    }

    setFormErrors(validation);

    if (Object.keys(validation).length > 0) {
      setError('Date invalide. Corecteaza campurile marcate. ✍️');
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
        <p className="lead">Manage {entity.label.toLowerCase()} with style — beautiful, simple, Parisian. 🇫🇷</p>
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
                <input
                  type="checkbox"
                  checked={Boolean(formValues[field.key])}
                  onChange={(event) => onChange(field.key, event.target.checked)}
                />
              ) : field.type === 'relationId' ? (
                <select
                  value={String(formValues[field.key] ?? '')}
                  onChange={(event) => onChange(field.key, event.target.value)}
                >
                  <option value="">-- select --</option>

                  {(relationOptions[field.relationName ?? field.key] ?? []).map((option) => (
                    <option key={option.id} value={String(option.id)}>
                      {option.label}
                    </option>
                  ))}
                </select>
              ) : field.type === 'select' ? (
                <select
                  value={String(formValues[field.key] ?? '')}
                  onChange={(event) => onChange(field.key, event.target.value)}
                >
                  <option value="">-- select --</option>

                  {(field.options ?? []).map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
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

      <p className="status">{isBusy ? 'Working... ⏳' : status}</p>
      {error ? <p className="error">{error}</p> : null}

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
        <h2>404 — Oops! 🇫🇷</h2>
        <p>Nu am gasit pagina. Poate lua un croissant si incercati din nou? 🥐</p>
        <p>
          <a href="/">Mergi la dashboard</a>
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
        <h2>500 — Oops! Ceva nu a mers. 💥</h2>
        <p>{message ?? 'A aparut o eroare pe server. Ne cerem scuze — revenim imediat.'}</p>
        <p>In timp ce rezolvam, incearca sa reimprospatezi sau contacteaza adminul. ☕️</p>
        <p>
          <a href="/">Mergi la dashboard</a>
        </p>
      </div>
    </main>
  );
}

function App() {
  const firstEntityPath = useMemo(() => `/entities/${entityConfigs[0].key}`, []);


  const getLoggedUser = () => {
    const fromLocalStorage = localStorage.getItem('loggedUser');
    const fromSessionStorage = sessionStorage.getItem('loggedUser');

    const rawUser = fromLocalStorage ?? fromSessionStorage;

    if (!rawUser) {
      return null;
    }

    return JSON.parse(rawUser);
  };

  const logout = () => {
    localStorage.removeItem('loggedUser');
    sessionStorage.removeItem('loggedUser');
    window.location.href = '/login';
  };

  function ProtectedRoute({ children }: { children: React.ReactNode }) {
    const user = getLoggedUser();

    if (!user) {
      return <Navigate to="/login" replace />;
    }

    return children;
  }

  return (
    <div className="layout">
      <aside className="sidebar">
        <h1 className="brand">🥐 La Petite Table</h1>
        <p className="sidebar-sub">Admin panel — Parisian UI</p>

        {entityConfigs.map((entity) => (
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

        <footer className="sidebar-footer">Made with ❤️ in Paris</footer>
      </aside>

      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
       <Route
         path="/"
         element={
           <ProtectedRoute>
             <Navigate to={firstEntityPath} replace />
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

      <footer className="global-footer">© La Petite Table — Admin</footer>
    </div>
  );
}

export default App;