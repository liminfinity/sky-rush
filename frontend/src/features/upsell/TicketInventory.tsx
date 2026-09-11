import { useEffect, useState } from 'react';
import { api, errorMessage } from '../../api/client';
export function TicketInventory({ revision }: { revision: number }) {
  const [count, setCount] = useState<number | null>(null),
    [error, setError] = useState('');
  useEffect(() => {
    const c = new AbortController();
    void api
      .tickets(c.signal)
      .then((v) => {
        setCount(v.tickets);
        setError('');
      })
      .catch((e) => {
        if (!c.signal.aborted) setError(errorMessage(e));
      });
    return () => c.abort();
  }, [revision]);
  if (count === 0 && !error) return null;
  return (
    <p
      className="ticket-inventory"
      title="Коллекция без реальных розыгрышей и выплат"
    >
      {error || `Демо-билеты: ${count ?? '…'}`}
    </p>
  );
}
