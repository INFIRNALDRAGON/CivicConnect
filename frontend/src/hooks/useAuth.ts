import { useContext } from 'react';
import { AuthContext, AuthValue } from '../context/AuthContext';

export const useAuth = (): AuthValue => {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used inside AuthProvider');
  return context;
};
