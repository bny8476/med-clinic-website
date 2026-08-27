import { create } from 'zustand';
import pharmacyService from '../utils/pharmacy/pharmacyService';
import { toast } from 'react-hot-toast';

export const useReturnsStore = create((set, get) => ({
  returnsList: [],
  loading: false,
  error: false,

  fetchReturns: async () => {
    set({ loading: true, error: false });
    try {
      const response = await pharmacyService.getAllReturns();
      if (response && response.success) {
        set({ returnsList: Array.isArray(response.data) ? response.data : [], loading: false });
      } else {
        set({ returnsList: [], loading: false });
      }
    } catch (error) {
      set({ error: true, returnsList: [], loading: false });
      toast.error('Failed to load returns');
    }
  },

  approveReturn: async (id) => {
    try {
      await pharmacyService.approveReturn(id);
      toast.success('Return approved');
      get().fetchReturns();
      return true;
    } catch (e) {
      toast.error('Approval failed');
      return false;
    }
  }
}));
