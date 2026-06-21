import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';

function RegisterPage() {
  const navigate = useNavigate();

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState('WAITER');
  const [error, setError] = useState('');
  const [isBusy, setIsBusy] = useState(false);

  const register = async (event: React.FormEvent) => {
    event.preventDefault();

    setIsBusy(true);
    setError('');

    try {
      const response = await fetch('/api/auth/register', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ username, password, role }),
      });

      if (!response.ok) {
        throw new Error('Nu s-a putut crea contul.');
      }

      navigate('/login');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Eroare la register.');
    } finally {
      setIsBusy(false);
    }
  };

  return (
    <main className="login-page">
      <form className="login-card" onSubmit={register}>
        <h1>🥐 Register</h1>
        <p>Create restaurant user account</p>

        {error ? <div className="login-error">{error}</div> : null}

        <label>
          Username
          <input
            type="text"
            value={username}
            onChange={(event) => setUsername(event.target.value)}
            required
          />
        </label>

        <label>
          Password
          <input
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            required
          />
        </label>

        <label>
          Role
          <select value={role} onChange={(event) => setRole(event.target.value)}>
            <option value="MANAGER">Manager</option>
            <option value="WAITER">Ospatar</option>
            <option value="BARTENDER">Barman</option>
          </select>
        </label>

        <button type="submit" disabled={isBusy}>
          {isBusy ? 'Creating account...' : 'Register'}
        </button>

        <p>
          Ai deja cont? <Link to="/login">Login</Link>
        </p>
      </form>
    </main>
  );
}

export default RegisterPage;