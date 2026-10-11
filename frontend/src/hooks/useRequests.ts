import { useCallback, useEffect, useRef, useState } from 'react';
import { api, ApiError } from '../utils/api';
import { Category, QueueFilters, Report, ServiceRequest } from '../types';

interface Resource<T> {
  data: T | null;
  error: ApiError | null;
  loading: boolean;
  reload: () => void;
  refresh: () => void;
}

const useResource = <T,>(load: () => Promise<T | null>, key: string, pollMs: number = 0): Resource<T> => {
  const [state, setState] = useState<{ data: T | null; error: ApiError | null; loading: boolean }>({
    data: null,
    error: null,
    loading: true
  });
  const loadRef = useRef(load);
  const latest = useRef(0);
  loadRef.current = load;

  const run = useCallback(async (silent: boolean) => {
    const ticket = ++latest.current;
    if (!silent) setState((current) => ({ ...current, loading: true, error: null }));
    try {
      const data = await loadRef.current();
      if (ticket === latest.current) setState({ data, error: null, loading: false });
    } catch (error) {
      if (ticket !== latest.current) return;
      setState((current) => (silent ? current : { data: null, error: error as ApiError, loading: false }));
    }
  }, []);

  useEffect(() => {
    run(false);
  }, [run, key]);

  useEffect(() => {
    if (!pollMs) return undefined;
    const timer = setInterval(() => run(true), pollMs);
    return () => clearInterval(timer);
  }, [run, pollMs]);

  return { ...state, reload: () => run(false), refresh: () => run(true) };
};

export const useCategories = () => useResource<Category[]>(() => api.get('/categories'), 'categories');

export const useMyRequests = (pollMs?: number) =>
  useResource<ServiceRequest[]>(() => api.get('/requests/mine'), 'mine', pollMs);

export const useQueue = (filters: QueueFilters, pollMs?: number) =>
  useResource<ServiceRequest[]>(() => api.get('/requests', { ...filters }), JSON.stringify(filters), pollMs);

export const useRequestDetail = (id: string, pollMs?: number) =>
  useResource<ServiceRequest>(() => api.get(`/requests/${id}`), `detail-${id}`, pollMs);

export const useReport = (params: { from: string; to: string; categoryId: string }, enabled: boolean) =>
  useResource<Report>(() => (enabled ? api.get('/reports/summary', params) : Promise.resolve(null)), JSON.stringify([enabled, params]));
