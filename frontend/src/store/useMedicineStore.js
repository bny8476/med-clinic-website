import { create } from 'zustand';
import { toast } from 'react-hot-toast';
import pharmacyService from '../utils/pharmacy/pharmacyService';

const DEBOUNCE_MS = 400;
let searchTimer = null;
let abortController = null;

export const useMedicineStore = create((set, get) => ({
  medicines: [],
  totalElements: 0,
  totalPages: 0,
  loading: false,
  error: null,
  page: 0,
  pageSize: 20,
  searchTerm: '',
  drugClassFilter: 'ALL',
  scheduleFilter: 'ALL',

  setPage: (page) => {
    set({ page });
    get().fetchMedicines();
  },

  setPageSize: (pageSize) => {
    set({ pageSize, page: 0 });
    get().fetchMedicines();
  },

  setSearch: (searchTerm) => {
    set({ searchTerm, page: 0 });
    if (searchTimer) clearTimeout(searchTimer);
    searchTimer = setTimeout(() => get().fetchMedicines(), DEBOUNCE_MS);
  },

  setDrugClassFilter: (drugClassFilter) => {
    set({ drugClassFilter, page: 0 });
    get().fetchMedicines();
  },

  setScheduleFilter: (scheduleFilter) => {
    set({ scheduleFilter, page: 0 });
    get().fetchMedicines();
  },

  fetchMedicines: async () => {
    const { page, pageSize, searchTerm, drugClassFilter, scheduleFilter } = get();
    if (abortController) abortController.abort();
    const ctrl = new AbortController();
    abortController = ctrl;

    set({ loading: true, error: null });
    try {
      const res = await pharmacyService.getMedicines({
        page,
        size: pageSize,
        search: searchTerm || undefined,
        drugClass: drugClassFilter !== 'ALL' ? drugClassFilter : undefined,
        schedule: scheduleFilter !== 'ALL' ? scheduleFilter : undefined,
      });
      
      if (ctrl.signal.aborted) return;

      const payload = res?.data ?? res;
      if (payload?.content) {
        set({
          medicines: payload.content,
          totalElements: payload.totalElements ?? 0,
          totalPages: payload.totalPages ?? 0,
          loading: false,
        });
      } else {
        const list = Array.isArray(payload) ? payload : [];
        set({ medicines: list, totalElements: list.length, totalPages: 1, loading: false });
      }
    } catch (err) {
      if (ctrl.signal.aborted) return;
      set({ error: 'Failed to load medicines', loading: false });
      toast.error('Failed to load medicines');
    }
  },

  createMedicine: async (formData) => {
    try {
      await pharmacyService.createMedicine(formData);
      toast.success('Medicine added successfully');
      get().fetchMedicines();
      return true;
    } catch {
      toast.error('Failed to add medicine');
      return false;
    }
  },

  updateMedicine: async (id, formData) => {
    try {
      await pharmacyService.updateMedicine(id, formData);
      toast.success('Medicine updated successfully');
      get().fetchMedicines();
      return true;
    } catch {
      toast.error('Failed to update medicine');
      return false;
    }
  },
}));
