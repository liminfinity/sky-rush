import { useCallback, useEffect, useRef, useState } from 'react';
import { api, errorMessage } from '../../api/client';
import type { Profile } from '../../api/types';
export function useProfile(completedRoundId?: string) {
  const [profile, setProfile] = useState<Profile | null>(null),
    [error, setError] = useState(''),
    [loading, setLoading] = useState(true),
    [equipping, setEquipping] = useState<string | null>(null);
  const controller = useRef<AbortController | null>(null),
    busy = useRef(false),
    mounted = useRef(true);
  const refresh = useCallback(async () => {
    if (busy.current) return;
    controller.current?.abort();
    const request = new AbortController();
    controller.current = request;
    setLoading(true);
    try {
      const value = await api.profile(request.signal);
      if (!request.signal.aborted) {
        setProfile(value);
        setError('');
      }
    } catch (e) {
      if (!request.signal.aborted) setError(errorMessage(e));
    } finally {
      if (!request.signal.aborted) setLoading(false);
    }
  }, []);
  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
      controller.current?.abort();
    };
  }, []);
  useEffect(() => {
    void refresh();
  }, [completedRoundId, refresh]);
  const equip = async (id: string) => {
    if (busy.current) return;
    busy.current = true;
    controller.current?.abort();
    const request = new AbortController();
    controller.current = request;
    setEquipping(id);
    setError('');
    let saved = false;
    try {
      const collection = await api.equip(id, request.signal);
      if (!request.signal.aborted) {
        setProfile((p) => (p ? { ...p, collection } : p));
        saved = true;
      }
    } catch (e) {
      if (!request.signal.aborted) setError(errorMessage(e));
    } finally {
      busy.current = false;
      if (mounted.current) {
        setEquipping(null);
        setLoading(false);
        if (saved) void refresh();
      }
    }
  };
  return { profile, error, loading, equipping, refresh, equip };
}
