import { useEffect, useRef, useState } from 'react';
import type { Round } from '../../api/types';
export function useGameAudio(round: Round | null) {
  const [enabled, setEnabled] = useState(false),
    [error, setError] = useState('');
  const context = useRef<AudioContext | null>(null),
    last = useRef(''),
    lastTone = useRef(-1);
  const toggle = async () => {
    if (enabled) {
      setEnabled(false);
      return;
    }
    try {
      context.current ??= new AudioContext();
      await context.current.resume();
      setEnabled(true);
      setError('');
    } catch {
      setError('Звук недоступен в этом браузере.');
    }
  };
  useEffect(() => {
    const key = round
      ? `${round.id}:${round.state}:${round.booster.active}:${round.completedLevel}`
      : '';
    if (key === last.current) return;
    last.current = key;
    if (!enabled || !round || !context.current) return;
    const ctx = context.current;
    if (ctx.currentTime - lastTone.current < 0.18) return;
    lastTone.current = ctx.currentTime;
    const osc = ctx.createOscillator(),
      gain = ctx.createGain();
    osc.type = 'sine';
    osc.frequency.value = round.state.startsWith('COMPLETED')
      ? 220
      : round.booster.active
        ? 660
        : 440;
    gain.gain.setValueAtTime(0.035, ctx.currentTime);
    gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.12);
    osc.connect(gain);
    gain.connect(ctx.destination);
    osc.start();
    osc.stop(ctx.currentTime + 0.12);
    osc.onended = () => {
      osc.disconnect();
      gain.disconnect();
    };
  }, [round, enabled]);
  useEffect(
    () => () => {
      const audio = context.current;
      context.current = null;
      if (audio && audio.state !== 'closed') {
        void audio.close().catch(() => {
          // Navigation may already have disposed the browser audio device.
          // The component is unmounted; there is no player action to recover here.
        });
      }
    },
    [],
  );
  return { enabled, toggle, error };
}
