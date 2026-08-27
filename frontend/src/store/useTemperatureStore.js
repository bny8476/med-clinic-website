import { create } from 'zustand';
import { toast } from 'react-hot-toast';
import pharmacyService from '../utils/pharmacy/pharmacyService';

export const useTemperatureStore = create((set, get) => ({
  units: [],
  breaches: [],
  loading: false,
  submitting: false,

  fetchAll: async () => {
    set({ loading: true });
    try {
      const [uRes, bRes] = await Promise.all([
        pharmacyService.getStorageUnits(),
        pharmacyService.getTemperatureBreaches(),
      ]);
      set({
        units: uRes?.data ?? [],
        breaches: bRes?.data ?? [],
      });
    } catch {
      toast.error('Failed to load temperature data');
    } finally {
      set({ loading: false });
    }
  },

  createUnit: async (payload) => {
    set({ submitting: true });
    try {
      const res = await pharmacyService.createStorageUnit(payload);
      if (res?.success) {
        toast.success('Storage unit created');
        get().fetchAll();
        return true;
      }
    } catch {
      toast.error('Failed to create storage unit');
    } finally {
      set({ submitting: false });
    }
    return false;
  },

  recordTemperature: async (payload) => {
    set({ submitting: true });
    try {
      const res = await pharmacyService.recordTemperature(payload);
      if (res?.success) {
        toast.success('Temperature recorded');
        get().fetchAll();
        return true;
      }
    } catch {
      toast.error('Failed to record temperature');
    } finally {
      set({ submitting: false });
    }
    return false;
  },

  resolveBreachAction: async (logId, action) => {
    try {
      await pharmacyService.resolveBreachAction(logId, action);
      toast.success('Corrective action saved');
      get().fetchAll();
      return true;
    } catch {
      toast.error('Failed to save corrective action');
    }
    return false;
  },
}));
