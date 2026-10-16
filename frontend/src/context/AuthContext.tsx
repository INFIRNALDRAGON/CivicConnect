import React, { createContext, useState } from 'react';
import { api, clearSession, getSession, saveSession } from '../utils/api';
import { Session, User } from '../types';

export interface AuthValue {
  user: User | null;
  login: (email: string, password: string) => Promise<User>;
  register: (details: { name: string; email: string; password: string }) => Promise<User>;
  logout: () => void;
}

export const AuthContext = createContext<AuthValue | null>(null);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [session, setSession] = useState<Session | null>(getSession);

  const start = async (path: string, body: Record<string, unknown>): Promise<User> => {
    const data = await api.post<Session>(path, body);
    const next = { token: data.token, user: data.user };
    saveSession(next);
    setSession(next);
    return next.user;
  };

  const value: AuthValue = {
    user: session?.user ?? null,
    login: (email, password) => start('/auth/login', { email, password }),
    register: (details) => start('/auth/register', details),
    logout: () => {
      clearSession();
      setSession(null);
    }
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};
