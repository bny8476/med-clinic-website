import { create } from 'zustand';
import { toast } from 'react-hot-toast';
import pharmacyService from '../utils/pharmacy/pharmacyService';

const DEBOUNCE_MS = 400;
let searchTimer = null;
let abortController = null;

export const usePatientStore = create((set, get) => ({
  patients: [],
  loading: false,
  error: null,
  searchTerm: '',

  setSearch: (searchTerm) => {
    set({ searchTerm });
    if (searchTimer) clearTimeout(searchTimer);
    searchTimer = setTimeout(() => get().fetchPatients(), DEBOUNCE_MS);
  },

  fetchPatients: async () => {
    if (abortController) abortController.abort();
    const ctrl = new AbortController();
    abortController = ctrl;

    set({ loading: true, error: null });
    try {
      const res = await pharmacyService.getPatients();
      if (ctrl.signal.aborted) return;
      const list = res?.data ?? res ?? [];
      set({ patients: Array.isArray(list) ? list : [], loading: false });
    } catch {
      if (ctrl.signal.aborted) return;
      set({ error: 'Failed to load patients', loading: false });
      toast.error('Failed to fetch patients');
    }
  },

  createPatient: async (formData) => {
    try {
      const res = await pharmacyService.createPatient(formData);
      if (res?.success) {
        toast.success('Patient added');
        get().fetchPatients();
        return true;
      }
    } catch {
      toast.error('Failed to add patient');
    }
    return false;
  },

  updatePatient: async (id, formData) => {
    try {
      const res = await pharmacyService.updatePatient(id, formData);
      if (res?.success) {
        toast.success('Patient updated');
        get().fetchPatients();
        return true;
      }
    } catch {
      toast.error('Failed to update patient');
    }
    return false;
  },

  deletePatient: async (id) => {
    try {
      const res = await pharmacyService.deletePatient(id);
      if (res?.success) {
        toast.success('Patient deleted');
        get().fetchPatients();
        return true;
      }
    } catch {
      toast.error('Failed to delete patient');
    }
    return false;
  }
}));
