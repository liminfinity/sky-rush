import type { StartRequest, Theme } from '../../api/types';
export interface SavedSession {
  roundId?: string;
  pending?: StartRequest;
  savedAt?: number;
}
let key = 'skyrush.session.v1';
export function accountStorage(userId: string) {
  key = `skyrush.session.v1.${userId}`;
}
const uuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
export function readSession(): SavedSession {
  try {
    const value = JSON.parse(localStorage.getItem(key) || '{}') as SavedSession;
    if (
      value.savedAt == null ||
      !Number.isFinite(value.savedAt) ||
      Date.now() - value.savedAt > 3600000 ||
      value.savedAt > Date.now() + 60000
    ) {
      localStorage.removeItem(key);
      return {};
    }
    if (value.roundId && uuid.test(value.roundId))
      return { roundId: value.roundId };
    const p = value.pending;
    if (
      p &&
      uuid.test(p.requestId) &&
      ['RED', 'GREEN'].includes(p.theme) &&
      typeof p.betOptionId === 'string'
    )
      return { pending: p };
  } catch {
    /* Storage can be blocked or stale. */
  }
  return {};
}
export function saveSession(value: SavedSession) {
  try {
    localStorage.setItem(
      key,
      JSON.stringify(
        value.roundId || value.pending ? { ...value, savedAt: Date.now() } : {},
      ),
    );
  } catch {
    /* The round stays usable in memory. */
  }
}
export function readTheme(): Theme {
  try {
    return localStorage.getItem('skyrush.theme') === 'RED' ? 'RED' : 'GREEN';
  } catch {
    return 'GREEN';
  }
}
export function saveTheme(theme: Theme) {
  try {
    localStorage.setItem('skyrush.theme', theme);
  } catch {
    /* Optional preference. */
  }
}
