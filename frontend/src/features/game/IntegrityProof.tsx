import { useState } from 'react';
import type { Round } from '../../api/types';
export function IntegrityProof({ round }: { round: Round }) {
  const [status, setStatus] = useState('');
  const proof = round.integrity;
  if (!proof) return null;
  const verify = async () => {
    try {
      const bytes = await crypto.subtle.digest(
        'SHA-256',
        new TextEncoder().encode(proof.reveal!),
      );
      const hash = Array.from(new Uint8Array(bytes), (b) =>
        b.toString(16).padStart(2, '0'),
      ).join('');
      setStatus(
        hash === proof.commitment
          ? 'Исход не изменён.'
          : 'Проверка не пройдена. Сохрани номер полёта.',
      );
    } catch {
      setStatus('Этот браузер не поддерживает проверку.');
    }
  };
  return (
    <details className="integrity-proof">
      <summary>Проверить исход</summary>
      <p>Отпечаток исхода до полёта. Не сертификат честности.</p>
      <code>{proof.commitment}</code>
      {proof.reveal ? (
        <>
          <p>После взрыва:</p>
          <code>{proof.reveal}</code>
          <button
            type="button"
            className="quiet-button"
            onClick={() => void verify()}
          >
            Проверить
          </button>
          <p role="status">{status}</p>
        </>
      ) : (
        <p>Проверка — после взрыва.</p>
      )}
    </details>
  );
}
