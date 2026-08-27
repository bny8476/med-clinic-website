import { create } from 'zustand';
import { devtools } from 'zustand/middleware';
import pharmacyService from '../utils/pharmacy/pharmacyService';

let searchTimer = null;
let abortController = null;

export const usePrescriptionStore = create(devtools((set, get) => ({
  prescriptions: [],
  _rawPrescriptions: [],
  prescriptionsLoading: false,
  prescriptionsError: false,
  prescriptionsSearchTerm: '',
  prescriptionsDateRange: { from: null, to: null },

  _applyFilter: () => {
    const { _rawPrescriptions, prescriptionsSearchTerm, prescriptionsDateRange } = get();
    const term = prescriptionsSearchTerm.toLowerCase();
    
    const filtered = _rawPrescriptions.filter(p => {
      const matchesSearch = !term || p.patientName?.toLowerCase().includes(term) || p.uhid?.toLowerCase().includes(term) || p.id?.toString().includes(term);
      
      let matchesFrom = true;
      let matchesTo = true;
      
      if (prescriptionsDateRange.from || prescriptionsDateRange.to) {
        const prDate = new Date(p.prescriptionDate);
        const normalizedPrDate = new Date(prDate.getFullYear(), prDate.getMonth(), prDate.getDate()).getTime();
        if (prescriptionsDateRange.from) {
           matchesFrom = normalizedPrDate >= new Date(prescriptionsDateRange.from.getFullYear(), prescriptionsDateRange.from.getMonth(), prescriptionsDateRange.from.getDate()).getTime();
        }
        if (prescriptionsDateRange.to) {
           matchesTo = normalizedPrDate <= new Date(prescriptionsDateRange.to.getFullYear(), prescriptionsDateRange.to.getMonth(), prescriptionsDateRange.to.getDate()).getTime();
        }
      }
      return matchesSearch && matchesFrom && matchesTo;
    });

    set({ prescriptions: filtered });
  },

  setPrescriptionsSearch: (term) => {
    set({ prescriptionsSearchTerm: term });
    if (searchTimer) clearTimeout(searchTimer);
    
    searchTimer = setTimeout(() => {
      get()._applyFilter();
    }, 400);
  },

  setPrescriptionsDateRange: (range) => {
    set({ prescriptionsDateRange: range });
    get()._applyFilter();
  },

  fetchPrescriptions: async () => {
    if (abortController) {
      abortController.abort();
    }
    abortController = new AbortController();

    set({ prescriptionsLoading: true, prescriptionsError: false });
    try {
      const res = await pharmacyService.api.get('/pharmacy/prescriptions/pending', {
        signal: abortController.signal
      });
      const data = res.data?.data || [];
      
      set({ _rawPrescriptions: data, prescriptionsLoading: false });
      get()._applyFilter();
    } catch (err) {
      if (err.name === 'CanceledError' || err.code === 'ERR_CANCELED') return;
      set({ prescriptionsError: true, prescriptionsLoading: false });
    }
  }
})));
