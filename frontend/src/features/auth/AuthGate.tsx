import { useEffect, useRef, useState } from 'react';
import { api, ApiError, errorMessage } from '../../api/client';
import type { Account } from '../../api/types';
import { HomePage } from '../../pages/HomePage';
import { SkyWorld } from '../../components/SkyWorld';
import { Balloon } from '../../components/Balloon';
import { setOfferAccount } from '../upsell/UpsellPrompt';
import { accountStorage } from '../game/storage';

function field(form: FormData, name: string): string {
  const value = form.get(name);
  return typeof value === 'string' ? value : '';
}

export function AuthGate() {
  const [account, setAccount] = useState<Account | null>(null),
    [loading, setLoading] = useState(true),
    [mode, setMode] = useState<'login' | 'register'>('login');
  const [error, setError] = useState(''),
    [busy, setBusy] = useState(false);
  const lock = useRef(false);
  const accept = (user: Account) => {
    accountStorage(user.id);
    setOfferAccount(user.id);
    setAccount(user);
  };
  useEffect(() => {
    let alive = true;
    const c = new AbortController();
    void api
      .me(c.signal)
      .then((user) => {
        if (alive) accept(user);
      })
      .catch((e) => {
        if (alive && !(e instanceof ApiError && e.status === 401))
          setError(errorMessage(e));
      })
      .finally(() => {
        if (alive) setLoading(false);
      });
    const expired = () => {
      setAccount(null);
      setError('Войди снова');
    };
    window.addEventListener('skyrush:unauthenticated', expired);
    return () => {
      alive = false;
      c.abort();
      window.removeEventListener('skyrush:unauthenticated', expired);
    };
  }, []);
  const logout = async () => {
    if (lock.current) return;
    lock.current = true;
    try {
      await api.logout();
      setAccount(null);
      setError('');
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      lock.current = false;
    }
  };
  if (account)
    return (
      <>
        {error && (
          <div role="alert" className="auth-error">
            {error}
          </div>
        )}
        <HomePage
          key={account.id}
          evaluator={account.evaluator}
          onLogout={() => void logout()}
        />
      </>
    );
  return (
    <div className="app-shell theme-green mode-auth">
      <SkyWorld theme="GREEN" />
      <main className="auth-screen">
        <div className="auth-balloon" aria-hidden="true">
          <Balloon />
        </div>
        <form
          className="auth-form"
          onSubmit={(event) => {
            event.preventDefault();
            const form = new FormData(event.currentTarget);
            void (async () => {
              if (lock.current) return;
              lock.current = true;
              setBusy(true);
              setError('');
              try {
                const user =
                  mode === 'login'
                    ? await api.login(
                        field(form, 'username'),
                        field(form, 'password'),
                      )
                    : await api.register(
                        field(form, 'username'),
                        field(form, 'displayName'),
                        field(form, 'password'),
                      );
                accept(user);
              } catch (e) {
                setError(errorMessage(e));
              } finally {
                lock.current = false;
                setBusy(false);
              }
            })();
          }}
        >
          <div className="brand">
            Sky<span>Rush</span>
          </div>
          <h1>{mode === 'login' ? 'Войти' : 'Создать аккаунт'}</h1>
          {loading ? (
            <p role="status">Загружаем…</p>
          ) : (
            <>
              <label>
                Логин
                <input
                  name="username"
                  autoComplete="username"
                  required
                  minLength={3}
                  maxLength={40}
                  pattern="[A-Za-z0-9_]+"
                  title="3–40 латинских букв, цифр или подчёркиваний"
                />
              </label>
              {mode === 'register' && (
                <label>
                  Имя
                  <input
                    name="displayName"
                    autoComplete="nickname"
                    required
                    maxLength={40}
                  />
                </label>
              )}
              <label>
                Пароль
                <input
                  name="password"
                  type="password"
                  autoComplete={
                    mode === 'login' ? 'current-password' : 'new-password'
                  }
                  required
                  minLength={8}
                  maxLength={72}
                />
              </label>
              {error && <p role="alert">{error}</p>}
              <button type="submit" className="primary-button" disabled={busy}>
                {busy
                  ? 'Подожди…'
                  : mode === 'login'
                    ? 'Войти'
                    : 'Создать аккаунт'}
              </button>
              <button
                type="button"
                className="quiet-button"
                disabled={busy}
                onClick={() => {
                  setMode(mode === 'login' ? 'register' : 'login');
                  setError('');
                }}
              >
                {mode === 'login' ? 'Создать аккаунт' : 'Войти'}
              </button>
              <small>Для жюри: demo / demo12345</small>
            </>
          )}
        </form>
      </main>
    </div>
  );
}
