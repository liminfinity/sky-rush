import { useEffect, useState } from 'react';
import { api, errorMessage } from '../../api/client';
import type {
  Achievement,
  DailyChallenge,
  SocialActivity,
} from '../../api/types';
export function useSocial(completedRoundId?: string, profileOpen = false) {
  const [activity, setActivity] = useState<SocialActivity | null>(null),
    [daily, setDaily] = useState<DailyChallenge | null>(null),
    [achievements, setAchievements] = useState<Achievement[]>([]),
    [error, setError] = useState(''),
    [achievementError, setAchievementError] = useState('');
  // One non-overlapping slow loop. Hidden tabs don't send heartbeats or feed polls.
  useEffect(() => {
    const c = new AbortController();
    let timer: ReturnType<typeof setTimeout>;
    let lastHeartbeat = -Infinity;
    async function poll() {
      try {
        if (!document.hidden) {
          const now = Date.now();
          const value =
            now - lastHeartbeat >= 25000
              ? await api.heartbeat(c.signal)
              : await api.activity(c.signal);
          if (now - lastHeartbeat >= 25000) lastHeartbeat = now;
          const today = await api.daily(c.signal);
          if (!c.signal.aborted) {
            setActivity(value);
            setDaily(today);
            setError('');
          }
        }
      } catch (e) {
        if (!c.signal.aborted) setError(errorMessage(e));
      } finally {
        if (!c.signal.aborted) timer = setTimeout(() => void poll(), 5000);
      }
    }
    void poll();
    return () => {
      c.abort();
      clearTimeout(timer);
    };
  }, []);
  useEffect(() => {
    const c = new AbortController();
    void api
      .achievements(c.signal)
      .then((v) => {
        if (!c.signal.aborted) {
          setAchievements(v);
          setAchievementError('');
        }
      })
      .catch((e) => {
        if (!c.signal.aborted) setAchievementError(errorMessage(e));
      });
    return () => c.abort();
  }, [completedRoundId, profileOpen]);
  return { activity, daily, achievements, error: error || achievementError };
}
