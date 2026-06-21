import { useEffect, useMemo, useState } from 'react';
import { NavLink, Navigate, Route, Routes, useLocation, useNavigate, useParams } from 'react-router-dom';
import { ApiError, api } from './api';
import { entityConfigByKey, entityConfigs, validateEntityForm, type EntityConfig } from './entities';
import type { EntityName } from './types';

type FormValues = Record<string, string | boolean>;
type Row = Record<string, any>;

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
  // relation options for select fields: relationName -> [{id, label}]
 type RelationOption = {
   id: number;
   label: string;
 };

 const [relationOptions, setRelationOptions] = useState<Record<string, RelationOption[]>>({});

  useEffect(() => {
    if (!entity) return;
    setFormValues(createInitialValues(entity));
    setFormErrors({});
    setEditingId(null);
    setRows([]);
    setStatus('Ready');
    setError('');
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
      if (successMessage) setStatus('✅ ' + successMessage);
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Unexpected error';
      setError(msg);
      if (err instanceof ApiError) setServerErrorPage(err);
    } finally {
      setIsBusy(false);
    }
  };

  const refreshAll = async () => {
    if (!entity) return;
    await withApi(async () => {
      const data = await api.getAll<Row>(entity.endpoint);
      setRows(data);
      setStatus(`Loaded ${data.length} ${entity.label.toLowerCase()}.`);
    });
  };

  useEffect(() => { refreshAll(); /* eslint-disable-next-line */ }, [entityKey]);

  // fetch relation options for relationId fields when entity loads
  useEffect(() => {
    if (!entity) return;
    const relFields = entity.fields.filter((f) => f.type === 'relationId');
    relFields.forEach(async (f) => {
      const relName = f.relationName ?? f.key;
      // try plural endpoint guesses: /{relName}s and /{relName}
      const guesses = [`/${relName}s`, `/${relName}`];
      for (const ep of guesses) {
        try {
          const data = await api.getAll<any>(ep);
          if (Array.isArray(data) && data.length > 0) {
            const opts = data.map((it) => ({ id: Number(it.id), label: it.name ?? it.username ?? it.tableNumber ?? String(it.id) }));
            setRelationOptions((prev) => ({ ...prev, [relName]: opts }));
            break;
          }
        } catch (e) {
          // ignore and try next guess
        }
      }
    });
  }, [entityKey, entity]);

  if (!entity) return <Navigate to="/404" replace />;

  const onChange = (key: string, value: string | boolean) => {
    setFormValues((prev) => ({ ...prev, [key]: value }));
  };

  // Populate form from a selected table row. Handles relation fields in the returned row.
  const populateFormFromRow = (row: Row) => {
    const values: FormValues = {};
    entity.fields.forEach((f) => {
      if (f.type === 'boolean') {
        values[f.key] = Boolean(row[f.key]);
      } else if (f.type === 'relationId') {
        // row may have relation object (category: {id:..}) or a plain id field (categoryId)
        const relName = f.relationName ?? f.key;
        if (row[relName] && typeof row[relName] === 'object' && row[relName].id !== undefined) {
          values[f.key] = String(row[relName].id);
        } else if (row[f.key] !== undefined) {
          values[f.key] = String(row[f.key]);
        } else {
          values[f.key] = '';
        }
      } else if (f.type === 'number') {
        values[f.key] = row[f.key] !== undefined ? String(row[f.key]) : '';
      } else {
        values[f.key] = row[f.key] !== undefined ? String(row[f.key]) : '';
      }
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
      if (editingId === targetId) clearForm();
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
              <span>{field.label}{field.required ? ' *' : ''}</span>
              {field.type === 'boolean' ? (
                <input
                  type="checkbox"
                  checked={Boolean(formValues[field.key])}
                  onChange={(event) => onChange(field.key, event.target.checked)}
                />
              ) : field.type === 'relationId' ? (
                // render a select with fetched options when available
                <select
                  value={String(formValues[field.key] ?? '')}
                  onChange={(e) => onChange(field.key, e.target.value)}
                >
                  <option value="">-- select --</option>
                 {(relationOptions[field.relationName ?? field.key] ?? []).map((opt: RelationOption) => (
                   <option key={opt.id} value={String(opt.id)}>
                     {opt.label} (#{opt.id})
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
          <button type="button" onClick={() => runCreateOrUpdate('create')} disabled={isBusy}>➕ Create</button>
          <button type="button" onClick={() => runCreateOrUpdate('update')} disabled={isBusy || !editingId}>✏️ Save</button>
          <button type="button" onClick={() => runDelete()} disabled={isBusy || !editingId}>🗑️ Delete</button>
          <button type="button" onClick={clearForm} disabled={isBusy}>✖️ Clear</button>
          <button type="button" onClick={refreshAll} disabled={isBusy}>🔄 Refresh</button>
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
                {entity.fields.map((f) => (<th key={f.key}>{f.label}</th>))}
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((r) => (
                <tr key={r.id} className={editingId === r.id ? 'row-active' : ''}>
                  <td>{r.id}</td>
                  {entity.fields.map((f) => (
                    <td key={f.key}>{
                      f.type === 'relationId' ? (r[f.relationName ?? f.key]?.id ?? r[f.key] ?? '') : String(r[f.key] ?? '')
                    }</td>
                  ))}
                  <td>
                    <button onClick={() => startEdit(r)}>Edit</button>
                    <button onClick={() => runDelete(Number(r.id))}>Delete</button>
                  </td>
                </tr>
              ))}
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
        <p><a href="/">Mergi la dashboard</a></p>
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
        <p><a href="/">Mergi la dashboard</a></p>
      </div>
    </main>
  );
}

function App() {
  const firstEntityPath = useMemo(() => `/entities/${entityConfigs[0].key}`, []);

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
        <footer className="sidebar-footer">Made with ❤️ in Paris</footer>
      </aside>

      <Routes>
        <Route path="/" element={<Navigate to={firstEntityPath} replace />} />
        <Route path="/entities/:entityKey" element={<EntityCrudPage />} />
        <Route path="/error/500" element={<ServerErrorPage />} />
        <Route path="/404" element={<NotFoundPage />} />
        <Route path="*" element={<Navigate to="/404" replace />} />
      </Routes>

      <footer className="global-footer">© La Petite Table — Admin</footer>
    </div>
  );
}

export default App;

