import { useState } from 'react';
//import { useNavigate } from 'react-router-dom';
import { Link, useNavigate } from 'react-router-dom';

type LoggedUser = {
  id: number;
  username: string;
  role: string;
};

function LoginPage() {
  const navigate = useNavigate();

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [rememberMe, setRememberMe] = useState(false);
  const [error, setError] = useState('');
  const [isBusy, setIsBusy] = useState(false);

  const login = async (event: React.FormEvent) => {
    event.preventDefault();

    setIsBusy(true);
    setError('');

    try {
      const response = await fetch('/api/auth/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ username, password }),
      });

      if (!response.ok) {
        throw new Error('Username sau parola gresite.');
      }

      const user: LoggedUser = await response.json();

      if (rememberMe) {
        localStorage.setItem('loggedUser', JSON.stringify(user));
      } else {
        sessionStorage.setItem('loggedUser', JSON.stringify(user));
      }

      navigate('/');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Eroare la login.');
    } finally {
      setIsBusy(false);
    }
  };

  return (
    <main className="login-page">
      <form className="login-card" onSubmit={login}>
        <h1>🥐 La Petite Table</h1>
        <p>Login into restaurant admin panel</p>

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

        <label className="remember-row">
          <input
            type="checkbox"
            checked={rememberMe}
            onChange={(event) => setRememberMe(event.target.checked)}
          />
          Remember me
        </label>

        <button type="submit" disabled={isBusy}>
          {isBusy ? 'Logging in...' : 'Login'}
        </button>
        <p>
          Nu ai cont? <Link to="/register">Register</Link>
        </p>

      </form>
    </main>
  );
}

export default LoginPage;