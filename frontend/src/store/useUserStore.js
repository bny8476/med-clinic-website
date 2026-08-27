import { create } from 'zustand';
import { toast } from 'react-hot-toast';
import api from '../utils/pharmacy/api';

const POLL_INTERVAL_MS = 30_000;
let pollTimer = null;
let abortController = null;

export const useUserStore = create((set, get) => ({
  users: [],
  loading: false,
  error: null,
  searchTerm: '',

  setSearch: (searchTerm) => set({ searchTerm }),

  fetchUsers: async () => {
    if (abortController) abortController.abort();
    const ctrl = new AbortController();
    abortController = ctrl;

    set({ loading: true, error: null });
    try {
      const res = await api.get('/auth/users');
      if (ctrl.signal.aborted) return;
      const list = res.data?.data ?? res.data ?? [];
      set({ users: Array.isArray(list) ? list : [], loading: false });
    } catch {
      if (ctrl.signal.aborted) return;
      set({ error: 'Failed to load users', loading: false });
      toast.error('Failed to fetch users');
    }
  },

  startPolling: () => {
    get().fetchUsers();
    if (pollTimer) clearInterval(pollTimer);
    pollTimer = setInterval(() => get().fetchUsers(), POLL_INTERVAL_MS);
  },

  stopPolling: () => {
    if (pollTimer) {
      clearInterval(pollTimer);
      pollTimer = null;
    }
    if (abortController) {
      abortController.abort();
    }
  },

  createUser: async (formData) => {
    try {
      const res = await api.post('/auth/users', formData);
      if (res.data?.success) {
        toast.success('User created!');
        get().fetchUsers();
        return { success: true, data: res.data };
      }
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to create user');
    }
    return { success: false };
  },

  updateUser: async (id, formData) => {
    try {
      const res = await api.put(`/auth/users/${id}`, formData);
      if (res.data?.success) {
        toast.success('User updated!');
        get().fetchUsers();
        return true;
      }
    } catch {
      toast.error('Failed to update user');
    }
    return false;
  },

  toggleUserStatus: async (user) => {
    try {
      await api.put(`/auth/users/${user.id}/status`);
      toast.success(`User ${user.status === 'ACTIVE' ? 'suspended' : 'activated'}`);
      get().fetchUsers();
      return true;
    } catch {
      toast.error('Failed to update status');
    }
    return false;
  },
}));
