import { useEffect, useMemo, useState } from 'react';
import { NavLink, Navigate, Route, Routes, useLocation, useNavigate, useParams } from 'react-router-dom';
import { ApiError, api } from './api';
import { entityConfigByKey, entityConfigs, validateEntityForm, type EntityConfig } from './entities';
import type { EntityName } from './types';

type FormValues = Record<string, string | boolean>;
type Row = Record<string, unknown>;

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

const pretty = (value: unknown) => JSON.stringify(value, null, 2);

function EntityCrudPage() {
  const { entityKey } = useParams();
  const navigate = useNavigate();
  const entity = entityConfigByKey[entityKey as EntityName];

  const [formValues, setFormValues] = useState<FormValues>(() => (entity ? createInitialValues(entity) : {}));
  const [formErrors, setFormErrors] = useState<Record<string, string>>({});
  const [selectedId, setSelectedId] = useState('');
  const [rows, setRows] = useState<Row[]>([]);
  const [status, setStatus] = useState('Ready');
  const [error, setError] = useState('');
  const [isBusy, setIsBusy] = useState(false);

  useEffect(() => {
    if (!entity) {
      return;
    }
    setFormValues(createInitialValues(entity));
    setFormErrors({});
    setSelectedId('');
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
      if (successMessage) {
        setStatus(successMessage);
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
    if (!entity) {
      return;
    }
    await withApi(async () => {
      const data = await api.getAll<Row>(entity.endpoint);
      setRows(data);
      setStatus(`Loaded ${data.length} ${entity.label.toLowerCase()}.`);
    });
  };

  useEffect(() => {
    refreshAll();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [entityKey]);

  if (!entity) {
    return <Navigate to="/404" replace />;
  }

  const onChange = (key: string, value: string | boolean) => {
    setFormValues((prev) => ({ ...prev, [key]: value }));
  };

  const runCreateOrUpdate = async (mode: 'create' | 'update') => {
    const validation = validateEntityForm(entity, formValues);

    if (mode === 'update') {
      const id = Number(selectedId);
      if (!Number.isInteger(id) || id <= 0) {
        validation.id = 'ID-ul pentru update trebuie sa fie un numar pozitiv.';
      }
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
        await api.update<Row>(entity.endpoint, Number(selectedId), payload);
      }
      await refreshAll();
    }, mode === 'create' ? 'Created successfully.' : 'Updated successfully.');
  };

  const runGetById = async () => {
    const id = Number(selectedId);
    if (!Number.isInteger(id) || id <= 0) {
      setFormErrors({ id: 'ID-ul trebuie sa fie un numar pozitiv.' });
      setError('Introdu un ID valid.');
      return;
    }

    setFormErrors({});
    await withApi(async () => {
      const item = await api.getById<Row>(entity.endpoint, id);
      setRows([item]);
    }, `Loaded ${entity.label.slice(0, -1)} #${id}.`);
  };

  const runDelete = async () => {
    const id = Number(selectedId);
    if (!Number.isInteger(id) || id <= 0) {
      setFormErrors({ id: 'ID-ul trebuie sa fie un numar pozitiv.' });
      setError('Introdu un ID valid.');
      return;
    }

    setFormErrors({});
    await withApi(async () => {
      await api.remove(entity.endpoint, id);
      await refreshAll();
    }, 'Deleted successfully.');
  };

  return (
    <main className="content">
      <h2>{entity.label}</h2>
      <div className="panel">
        <h3>Formular Create / Update</h3>
        <div className="form-grid">
          {entity.fields.map((field) => (
            <label key={field.key} className="field">
              <span>{field.label}</span>
              {field.type === 'boolean' ? (
                <input
                  type="checkbox"
                  checked={Boolean(formValues[field.key])}
                  onChange={(event) => onChange(field.key, event.target.checked)}
                />
              ) : (
                <input
                  type={field.type === 'text' ? 'text' : 'number'}
                  step={field.integer ? '1' : 'any'}
                  value={String(formValues[field.key] ?? '')}
                  onChange={(event) => onChange(field.key, event.target.value)}
                  placeholder={field.required ? 'required' : ''}
                />
              )}
              {formErrors[field.key] ? <small className="error">{formErrors[field.key]}</small> : null}
            </label>
          ))}
        </div>

        <label className="field">
          <span>ID (pentru Read by ID / Update / Delete)</span>
          <input
            type="number"
            min="1"
            value={selectedId}
            onChange={(event) => setSelectedId(event.target.value)}
          />
          {formErrors.id ? <small className="error">{formErrors.id}</small> : null}
        </label>

        <div className="actions">
          <button type="button" onClick={refreshAll} disabled={isBusy}>Read All</button>
          <button type="button" onClick={runGetById} disabled={isBusy}>Read by ID</button>
          <button type="button" onClick={() => runCreateOrUpdate('create')} disabled={isBusy}>Create</button>
          <button type="button" onClick={() => runCreateOrUpdate('update')} disabled={isBusy}>Update</button>
          <button type="button" onClick={runDelete} disabled={isBusy}>Delete</button>
        </div>
      </div>

      <p className="status">{isBusy ? 'Working...' : status}</p>
      {error ? <p className="error">{error}</p> : null}

      <div className="result">
        <pre>{pretty(rows)}</pre>
      </div>
    </main>
  );
}

function NotFoundPage() {
  return (
    <main className="content">
      <h2>404 - Page not found</h2>
      <p>Ruta ceruta nu exista.</p>
    </main>
  );
}

function ServerErrorPage() {
  const location = useLocation();
  const message = (location.state as { message?: string } | null)?.message;

  return (
    <main className="content">
      <h2>500 - Server error</h2>
      <p>{message ?? 'A aparut o eroare pe server.'}</p>
    </main>
  );
}

function App() {
  const firstEntityPath = useMemo(() => `/entities/${entityConfigs[0].key}`, []);

  return (
    <div className="layout">
      <aside className="sidebar">
        <h1>Restaurant Admin</h1>
        {entityConfigs.map((entity) => (
          <NavLink
            key={entity.key}
            to={`/entities/${entity.key}`}
            className={({ isActive }) => (isActive ? 'tab active' : 'tab')}
          >
            {entity.label}
          </NavLink>
        ))}
      </aside>

      <Routes>
        <Route path="/" element={<Navigate to={firstEntityPath} replace />} />
        <Route path="/entities/:entityKey" element={<EntityCrudPage />} />
        <Route path="/error/500" element={<ServerErrorPage />} />
        <Route path="/404" element={<NotFoundPage />} />
        <Route path="*" element={<Navigate to="/404" replace />} />
      </Routes>
    </div>
  );
}

export default App;

