import { create } from 'zustand';
import { toast } from 'react-hot-toast';
import pharmacyService from '../utils/pharmacy/pharmacyService';

export const useExpiryStore = create((set, get) => ({
  batches: [],
  summary: null,
  returns: [],
  loading: false,
  submitting: false,
  error: null,

  fetchAll: async () => {
    set({ loading: true, error: null });
    try {
      const [bRes, sRes, rRes] = await Promise.all([
        pharmacyService.getExpiryBatches(),
        pharmacyService.getExpirySummary(),
        pharmacyService.getExpiryReturns(),
      ]);
      set({
        batches: bRes?.data ?? [],
        summary: sRes?.data ?? null,
        returns: rRes?.data ?? [],
      });
    } catch {
      set({ error: 'Failed to load expiry data' });
      toast.error('Failed to load expiry data');
    } finally {
      set({ loading: false });
    }
  },

  initiateReturn: async (payload) => {
    set({ submitting: true });
    try {
      const res = await pharmacyService.initiateExpiryReturn(payload);
      if (res?.success) {
        toast.success('Return initiated successfully');
        get().fetchAll();
        return true;
      }
    } catch {
      toast.error('Failed to initiate return');
    } finally {
      set({ submitting: false });
    }
    return false;
  },
}));
