import { useEffect, useRef, useState } from 'react';
import { api, ApiError, errorMessage } from '../../api/client';
import { isCompleted, type Round } from '../../api/types';

/** One serialized request loop owns state reads and cashout. No timer-based game math. */
export function useRoundPolling(
  round: Round | null,
  accept: (round: Round) => void,
  refreshWallet: () => void,
) {
  const [busy, setBusy] = useState(false);
  const [connectionError, setConnectionError] = useState('');
  const [commandError, setCommandError] = useState('');
  const [missing, setMissing] = useState(false);
  const current = useRef(round);
  const callbacks = useRef({ accept, refreshWallet });
  const queue = useRef<(() => void) | null>(null);
  current.current = round;
  callbacks.current = { accept, refreshWallet };
  const id = round?.id;
  const complete = round ? isCompleted(round) : false;

  useEffect(() => {
    setBusy(false);
    setConnectionError('');
    setCommandError('');
    setMissing(false);
    if (!id || complete) return;
    let disposed = false;
    let running = false;
    let cashoutQueued = false;
    let commandLocked = false;
    let lastState = current.current?.state;
    let timer: ReturnType<typeof setTimeout> | undefined;
    const controller = new AbortController();
    const run = async () => {
      if (disposed || running) return;
      running = true;
      const command = cashoutQueued;
      cashoutQueued = false;
      let stop = false;
      try {
        const state = command
          ? await api.cashout(id, controller.signal)
          : await api.state(id, controller.signal);
        if (disposed) return;
        callbacks.current.accept(state);
        setConnectionError('');
        if (command || state.state !== lastState)
          callbacks.current.refreshWallet();
        lastState = state.state;
        if (!cashoutQueued) {
          commandLocked = false;
          setBusy(false);
        }
        stop = isCompleted(state);
      } catch (error) {
        if (disposed) return;
        if (error instanceof ApiError && error.code === 'ROUND_NOT_FOUND') {
          setMissing(true);
          stop = true;
        }
        if (command) setCommandError(errorMessage(error));
        else setConnectionError(errorMessage(error));
        // An uncertain cashout is reconciled by a state read before the button is re-enabled.
      } finally {
        running = false;
        if (!disposed && !stop)
          timer = setTimeout(
            () => void run(),
            cashoutQueued || command ? 0 : 450,
          );
      }
    };
    queue.current = () => {
      if (
        disposed ||
        commandLocked ||
        !current.current?.canCashout ||
        isCompleted(current.current)
      )
        return;
      commandLocked = true;
      cashoutQueued = true;
      setBusy(true);
      setCommandError('');
      clearTimeout(timer);
      if (!running) void run();
    };
    timer = setTimeout(() => void run(), 450);
    return () => {
      disposed = true;
      controller.abort();
      clearTimeout(timer);
      queue.current = null;
    };
  }, [id, complete]);
  return {
    cashout: () => queue.current?.(),
    busy,
    connectionError,
    commandError,
    missing,
  };
}
