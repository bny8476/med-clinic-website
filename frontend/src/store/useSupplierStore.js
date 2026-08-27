import { create } from 'zustand';
import { toast } from 'react-hot-toast';
import pharmacyService from '../utils/pharmacy/pharmacyService';

const DEBOUNCE_MS = 400;
let searchTimer = null;
let abortController = null;

export const useSupplierStore = create((set, get) => ({
  suppliers: [],
  loading: false,
  error: null,
  searchTerm: '',
  filterType: '',
  filterStatus: '',

  setSearch: (searchTerm) => {
    set({ searchTerm });
    if (searchTimer) clearTimeout(searchTimer);
    searchTimer = setTimeout(() => get().fetchSuppliers(), DEBOUNCE_MS);
  },

  setFilterType: (filterType) => {
    set({ filterType });
    get().fetchSuppliers();
  },

  setFilterStatus: (filterStatus) => {
    set({ filterStatus });
    get().fetchSuppliers();
  },

  fetchSuppliers: async () => {
    const { searchTerm, filterType, filterStatus } = get();
    if (abortController) abortController.abort();
    const ctrl = new AbortController();
    abortController = ctrl;

    set({ loading: true, error: null });
    try {
      const res = await pharmacyService.getSuppliers({
        search: searchTerm || undefined,
        type: filterType || undefined,
        status: filterStatus || undefined,
      });
      if (ctrl.signal.aborted) return;
      const list = res?.data?.content ?? res?.data ?? res ?? [];
      set({ suppliers: Array.isArray(list) ? list : [], loading: false });
    } catch {
      if (ctrl.signal.aborted) return;
      set({ error: 'Failed to load suppliers', loading: false });
      toast.error('Failed to load suppliers');
    }
  },

  createSupplier: async (formData) => {
    try {
      const res = await pharmacyService.createSupplier(formData);
      if (res?.success) {
        toast.success('Supplier added!');
        get().fetchSuppliers();
        return true;
      }
    } catch {
      toast.error('Failed to add supplier');
    }
    return false;
  },

  updateSupplier: async (id, formData) => {
    try {
      const res = await pharmacyService.updateSupplier(id, formData);
      if (res?.success) {
        toast.success('Supplier updated!');
        get().fetchSuppliers();
        return true;
      }
    } catch {
      toast.error('Failed to update supplier');
    }
    return false;
  },

  deleteSupplier: async (id) => {
    try {
      const res = await pharmacyService.deleteSupplier(id);
      if (res?.success) {
        toast.success('Supplier deleted');
        get().fetchSuppliers();
        return true;
      }
    } catch {
      toast.error('Failed to delete supplier');
    }
    return false;
  },
}));
