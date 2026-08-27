import { create } from 'zustand';
import { devtools } from 'zustand/middleware';
import pharmacyService from '../utils/pharmacy/pharmacyService';

let searchTimer = null;
let abortController = null;

export const useWorklistStore = create(devtools((set, get) => ({
  worklists: [],
  _rawWorklists: [],
  worklistsLoading: false,
  worklistsError: false,
  worklistsSearchTerm: '',
  worklistsDateRange: { from: null, to: null },

  _applyFilter: () => {
    const { _rawWorklists, worklistsSearchTerm, worklistsDateRange } = get();
    const term = worklistsSearchTerm.toLowerCase();
    
    const filtered = _rawWorklists.filter(w => {
      const matchesSearch = !term || w.patientName?.toLowerCase().includes(term) || w.prescriptionNumber?.toLowerCase().includes(term) || w.id?.toString().includes(term);
      
      let matchesFrom = true;
      let matchesTo = true;
      
      if (worklistsDateRange.from || worklistsDateRange.to) {
        const prDate = new Date(w.prescriptionDate);
        const normalizedPrDate = new Date(prDate.getFullYear(), prDate.getMonth(), prDate.getDate()).getTime();
        if (worklistsDateRange.from) {
           matchesFrom = normalizedPrDate >= new Date(worklistsDateRange.from.getFullYear(), worklistsDateRange.from.getMonth(), worklistsDateRange.from.getDate()).getTime();
        }
        if (worklistsDateRange.to) {
           matchesTo = normalizedPrDate <= new Date(worklistsDateRange.to.getFullYear(), worklistsDateRange.to.getMonth(), worklistsDateRange.to.getDate()).getTime();
        }
      }
      return matchesSearch && matchesFrom && matchesTo;
    });

    set({ worklists: filtered });
  },

  setWorklistsSearch: (term) => {
    set({ worklistsSearchTerm: term });
    if (searchTimer) clearTimeout(searchTimer);
    
    searchTimer = setTimeout(() => {
      get()._applyFilter();
    }, 400);
  },

  setWorklistsDateRange: (range) => {
    set({ worklistsDateRange: range });
    get()._applyFilter();
  },

  fetchWorklists: async () => {
    if (abortController) {
      abortController.abort();
    }
    abortController = new AbortController();

    set({ worklistsLoading: true, worklistsError: false });
    try {
      const res = await pharmacyService.api.get('/pharmacy/prescriptions/pending', {
        signal: abortController.signal
      });
      const data = res.data?.data || [];
      
      set({ _rawWorklists: data, worklistsLoading: false });
      get()._applyFilter();
    } catch (err) {
      if (err.name === 'CanceledError' || err.code === 'ERR_CANCELED') return;
      set({ worklistsError: true, worklistsLoading: false });
    }
  }
})));
