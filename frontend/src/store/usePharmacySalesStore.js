import { create } from 'zustand';
import { devtools } from 'zustand/middleware';
import pharmacyService from '../utils/pharmacy/pharmacyService';

let searchTimer = null;
let abortController = null;

export const usePharmacySalesStore = create(devtools((set, get) => ({
  salesList: [],
  salesLoading: false,
  salesError: false,
  salesPage: 0,
  salesTotalElements: 0,
  salesTotalPages: 0,
  salesSearchTerm: '',
  salesDateRange: { from: null, to: null },

  setSalesSearch: (term) => {
    set({ salesSearchTerm: term });
    if (searchTimer) clearTimeout(searchTimer);
    searchTimer = setTimeout(() => {
      set({ salesPage: 0 });
      get().fetchSales();
    }, 400);
  },

  setSalesDateRange: (range) => {
    set({ salesDateRange: range, salesPage: 0 });
    get().fetchSales();
  },

  setSalesPage: (page) => {
    set({ salesPage: page });
    get().fetchSales();
  },

  fetchSales: async () => {
    const { salesPage, salesSearchTerm, salesDateRange } = get();
    
    if (abortController) {
      abortController.abort();
    }
    abortController = new AbortController();
    
    set({ salesLoading: true, salesError: false });
    try {
      const params = {
        page: salesPage,
        size: 20,
        searchTerm: salesSearchTerm || '',
        fromDate: salesDateRange.from ? salesDateRange.from.toISOString() : '',
        toDate: salesDateRange.to ? salesDateRange.to.toISOString() : ''
      };
      
      const res = await pharmacyService.api.get('/pharmacy/sales', { 
        params,
        signal: abortController.signal
      });
      
      set({ 
        salesList: res.data?.data?.content || [],
        salesTotalElements: res.data?.data?.totalElements || 0,
        salesTotalPages: res.data?.data?.totalPages || 0,
        salesLoading: false 
      });
    } catch (err) {
      if (err.name === 'CanceledError' || err.code === 'ERR_CANCELED') return;
      set({ salesError: true, salesLoading: false });
    }
  }
})));
