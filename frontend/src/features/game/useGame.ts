import { useCallback, useEffect, useRef, useState } from 'react';
import { api, ApiError, errorMessage } from '../../api/client';
import {
  isCompleted,
  type Balance,
  type PublicConfig,
  type Round,
  type StartRequest,
  type Theme,
  type User,
} from '../../api/types';
import { readSession, readTheme, saveSession, saveTheme } from './storage';
import { useRoundPolling } from './useRoundPolling';

export function useGame() {
  const [theme, setThemeValue] = useState<Theme>(readTheme);
  const [user, setUser] = useState<User | null>(null);
  const [balance, setBalance] = useState<Balance | null>(null);
  const [config, setConfig] = useState<PublicConfig | null>(null);
  const [round, setRound] = useState<Round | null>(null);
  const [selected, setSelected] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [starting, setStarting] = useState(false);
  const [error, setError] = useState('');
  const [dataError, setDataError] = useState('');
  const [pending, setPending] = useState<StartRequest | undefined>(
    () => readSession().pending,
  );
  const [recovering, setRecovering] = useState(() =>
    Boolean(readSession().roundId),
  );
  const [historyRevision, setHistoryRevision] = useState(0);
  const [resultVisible, setResultVisible] = useState(false);
  const alive = useRef(false);
  const startLock = useRef(false);
  const lifecycle = useRef<AbortController | null>(null);
  const walletRevision = useRef(0);

  const refreshWallet = useCallback(async () => {
    const revision = ++walletRevision.current;
    const signal = lifecycle.current?.signal;
    try {
      const value = await api.balance(signal);
      if (
        alive.current &&
        !signal?.aborted &&
        revision === walletRevision.current
      )
        setBalance(value);
    } catch (e) {
      if (alive.current && !signal?.aborted) setDataError(errorMessage(e));
    }
  }, []);
  const refreshData = useCallback(async () => {
    const signal = lifecycle.current?.signal;
    const revision = ++walletRevision.current;
    const results = await Promise.allSettled([
      api.user(signal),
      api.balance(signal),
      api.config(signal),
    ]);
    if (!alive.current || signal?.aborted) return;
    if (results[0].status === 'fulfilled') setUser(results[0].value);
    if (
      results[1].status === 'fulfilled' &&
      revision === walletRevision.current
    )
      setBalance(results[1].value);
    if (results[2].status === 'fulfilled') setConfig(results[2].value);
    const failure = results.find((r) => r.status === 'rejected');
    setDataError(
      failure?.status === 'rejected' ? errorMessage(failure.reason) : '',
    );
    setLoading(false);
  }, []);
  const accept = useCallback((next: Round) => {
    if (!alive.current) return;
    saveSession({ roundId: next.id });
    saveTheme(next.theme);
    setThemeValue(next.theme);
    setRound((previous) => {
      if (previous?.id === next.id) {
        if (
          Date.parse(previous.serverTime) > Date.parse(next.serverTime) ||
          isCompleted(previous)
        )
          return previous;
        if (previous.state === 'CASHED_OUT' && next.state === 'ACTIVE')
          return previous;
      }
      return next;
    });
    setRecovering(false);
    setPending(undefined);
    setError('');
  }, []);
  const restore = useCallback(async () => {
    const saved = readSession();
    const signal = lifecycle.current?.signal;
    if (!saved.roundId) {
      if (saved.pending) {
        setRecovering(false);
        return;
      }
      try {
        const found = await api.active(signal);
        if (alive.current && !signal?.aborted && found?.round)
          accept(found.round);
      } catch (e) {
        if (alive.current && !signal?.aborted) setError(errorMessage(e));
      }
      if (!signal?.aborted) setRecovering(false);
      return;
    }
    setRecovering(true);
    try {
      const value = await api.state(saved.roundId, signal);
      if (!signal?.aborted) accept(value);
    } catch (e) {
      if (!alive.current || signal?.aborted) return;
      if (e instanceof ApiError && e.code === 'ROUND_NOT_FOUND') {
        saveSession({});
        setRecovering(false);
      }
      setError(errorMessage(e));
    }
  }, [accept]);
  useEffect(() => {
    alive.current = true;
    lifecycle.current = new AbortController();
    void refreshData();
    void restore();
    return () => {
      alive.current = false;
      lifecycle.current?.abort();
    };
  }, [refreshData, restore]);
  const polling = useRoundPolling(round, accept, () => void refreshWallet());
  const completedId = round && isCompleted(round) ? round.id : null;
  useEffect(() => {
    if (!completedId) {
      setResultVisible(false);
      return;
    }
    void refreshWallet();
    setHistoryRevision((value) => value + 1);
    const timer = setTimeout(() => setResultVisible(true), 1100);
    return () => clearTimeout(timer);
  }, [completedId, refreshWallet]);

  const start = async (repeat?: Round) => {
    if (startLock.current) return;
    if (repeat) {
      if (!isCompleted(repeat)) return;
      // Read current configuration for the displayed confirmation; server validates and prices the new round.
      startLock.current = true;
      setStarting(true);
      try {
        const [freshConfig, freshBalance] = await Promise.all([
          api.config(lifecycle.current?.signal),
          api.balance(lifecycle.current?.signal),
        ]);
        const nextBet = freshConfig.betOptions.find(
          (b) => b.id === repeat.betOptionId,
        );
        if (!nextBet || nextBet.stake > freshBalance.wallet.bonusBalance) {
          setError(
            nextBet
              ? 'Не хватает бонусов'
              : 'Ставка недоступна. Выбери другую.',
          );
          return;
        }
        setConfig(freshConfig);
        setBalance(freshBalance);
      } catch (e) {
        setError(errorMessage(e));
        return;
      } finally {
        startLock.current = false;
        setStarting(false);
      }
    }
    const bet = config?.betOptions.find((option) => option.id === selected);
    if (
      !repeat &&
      !pending &&
      (!bet || !balance || bet.stake > balance.wallet.bonusBalance || dataError)
    )
      return;
    const command = pending || {
      requestId: crypto.randomUUID(),
      theme: repeat?.theme ?? theme,
      betOptionId: repeat?.betOptionId ?? bet!.id,
    };
    startLock.current = true;
    setStarting(true);
    setError('');
    // Persist before the POST: a lost response can be retried with the same idempotency key, even after refresh.
    setPending(command);
    saveSession({ pending: command });
    try {
      accept(await api.start(command, lifecycle.current?.signal));
      void refreshWallet();
    } catch (e) {
      if (!alive.current) return;
      setError(errorMessage(e));
      if (e instanceof ApiError && e.status >= 400 && e.status < 500) {
        setPending(undefined);
        saveSession({});
      }
      void refreshData();
    } finally {
      startLock.current = false;
      if (alive.current) setStarting(false);
    }
  };
  const playAgain = useCallback(() => {
    saveSession({});
    setRound(null);
    setResultVisible(false);
    setPending(undefined);
    setSelected(null);
    setError('');
    void refreshData();
  }, [refreshData]);
  const chooseTheme = (value: Theme) => {
    if (round || starting || pending) return;
    setThemeValue(value);
    saveTheme(value);
  };
  return {
    theme,
    chooseTheme,
    user,
    balance,
    config,
    round,
    selected,
    setSelected,
    loading,
    starting,
    error,
    dataError,
    pending,
    recovering,
    restore,
    start,
    playAgain,
    refreshData,
    historyRevision,
    resultVisible,
    polling,
  };
}
