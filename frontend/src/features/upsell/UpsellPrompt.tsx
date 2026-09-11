import { useEffect, useRef, useState } from 'react';
import { api, errorMessage } from '../../api/client';
import type { Round, TicketOffer } from '../../api/types';
import { Modal } from '../../components/Modal';
import { Notice } from '../../components/Notice';
import { amount } from '../../utils/format';
let memorySession: string | undefined;
let offerAccount = '';
export function setOfferAccount(id: string) {
  if (offerAccount !== id) {
    offerAccount = id;
    memorySession = undefined;
    memorySeen = false;
  }
}
function storageKey(kind: string) {
  return `skyrush.offer-${kind}${offerAccount ? '.' + offerAccount : ''}`;
}
function sessionId() {
  try {
    let id = sessionStorage.getItem(storageKey('session'));
    if (!id) {
      id = crypto.randomUUID();
      sessionStorage.setItem(storageKey('session'), id);
    }
    return id;
  } catch {
    return (memorySession ??= crypto.randomUUID());
  }
}
let memorySeen = false;
function seen() {
  try {
    return sessionStorage.getItem(storageKey('seen')) === 'true';
  } catch {
    return memorySeen;
  }
}
function markSeen() {
  memorySeen = true;
  try {
    sessionStorage.setItem(storageKey('seen'), 'true');
  } catch {
    /* Session-only fallback. */
  }
}
export function UpsellPrompt({
  round,
  onOpen,
  onBalance,
}: {
  round: Round;
  onOpen: (open: boolean) => void;
  onBalance: () => void;
}) {
  const [offer, setOffer] = useState<TicketOffer | null>(null),
    [error, setError] = useState(''),
    [busy, setBusy] = useState(false),
    [seconds, setSeconds] = useState(0);
  const lock = useRef(false),
    callbacks = useRef({ onOpen, onBalance });
  callbacks.current = { onOpen, onBalance };
  useEffect(() => {
    if (round.state !== 'COMPLETED_WIN' || seen()) return;
    let disposed = false;
    const controller = new AbortController();
    void api
      .offer(sessionId(), round.id, controller.signal)
      .then((value) => {
        if (
          disposed ||
          !value.offer ||
          value.offer.status !== 'OFFERED' ||
          seen()
        )
          return;
        markSeen();
        setOffer(value.offer);
        setSeconds(value.offer.remainingSeconds);
        callbacks.current.onOpen(true);
      })
      .catch((e) => {
        if (!disposed) setError(errorMessage(e));
      });
    return () => {
      disposed = true;
      controller.abort();
      callbacks.current.onOpen(false);
    };
  }, [round.id, round.state]);
  useEffect(() => {
    if (!offer) return;
    const timer = setInterval(
      () => setSeconds((s) => Math.max(0, s - 1)),
      1000,
    );
    return () => clearInterval(timer);
  }, [offer]);
  const close = () => {
    setOffer(null);
    callbacks.current.onOpen(false);
  };
  useEffect(() => {
    if (offer && seconds === 0 && !busy) close();
  }, [seconds, busy, offer]);
  const decide = async (accept: boolean) => {
    if (!offer || lock.current) return;
    lock.current = true;
    setBusy(true);
    setError('');
    try {
      await api.decide(offer.id, sessionId(), accept);
      callbacks.current.onBalance();
      close();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      lock.current = false;
      setBusy(false);
    }
  };
  if (!offer)
    return error ? <Notice>{error} Билеты пока недоступны.</Notice> : null;
  return (
    <Modal
      title="Демо-билеты"
      onClose={() => {
        if (!busy) void decide(false);
      }}
    >
      <div className="upsell">
        <p>Без розыгрышей и призов.</p>
        <h3>
          {offer.quantity} шт. {amount(offer.total)} Б
        </h3>
        {error && <Notice>{error}</Notice>}
        <button
          type="button"
          className="primary-button"
          disabled={busy || seconds === 0}
          onClick={() => void decide(true)}
        >
          {busy ? 'Подожди…' : 'Купить'}
        </button>
        <button
          type="button"
          className="quiet-button"
          disabled={busy}
          onClick={() => void decide(false)}
        >
          Нет, спасибо
        </button>
        <p className="small-note">Закроется через {seconds} с</p>
      </div>
    </Modal>
  );
}
