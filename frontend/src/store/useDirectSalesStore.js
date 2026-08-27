import { create } from 'zustand';
import { devtools } from 'zustand/middleware';
import pharmacyService from '../utils/pharmacy/pharmacyService';

let searchTimer = null;
let abortController = null;

export const useDirectSalesStore = create(devtools((set, get) => ({
  directSalesList: [],
  directSalesLoading: false,
  directSalesError: false,
  directSalesPage: 0,
  directSalesTotalElements: 0,
  directSalesTotalPages: 0,
  directSalesSearchTerm: '',
  directSalesDateRange: { from: null, to: null },

  setDirectSalesSearch: (term) => {
    set({ directSalesSearchTerm: term });
    if (searchTimer) clearTimeout(searchTimer);
    
    searchTimer = setTimeout(() => {
      set({ directSalesPage: 0 });
      get().fetchDirectSales();
    }, 400);
  },

  setDirectSalesDateRange: (range) => {
    set({ directSalesDateRange: range, directSalesPage: 0 });
    get().fetchDirectSales();
  },

  setDirectSalesPage: (page) => {
    set({ directSalesPage: page });
    get().fetchDirectSales();
  },

  fetchDirectSales: async () => {
    const { directSalesPage, directSalesSearchTerm, directSalesDateRange } = get();
    
    if (abortController) {
      abortController.abort();
    }
    abortController = new AbortController();

    set({ directSalesLoading: true, directSalesError: false });
    try {
      const params = {
        page: directSalesPage,
        size: 20, 
        searchTerm: directSalesSearchTerm || '',
        fromDate: directSalesDateRange.from ? directSalesDateRange.from.toISOString() : '',
        toDate: directSalesDateRange.to ? directSalesDateRange.to.toISOString() : '',
        billType: 'OTC'
      };
      
      const res = await pharmacyService.api.get('/pharmacy/sales', { 
        params,
        signal: abortController.signal
      });
      
      set({ 
        directSalesList: res.data?.data?.content || [],
        directSalesTotalElements: res.data?.data?.totalElements || 0,
        directSalesTotalPages: res.data?.data?.totalPages || 0,
        directSalesLoading: false 
      });
    } catch (err) {
      if (err.name === 'CanceledError' || err.code === 'ERR_CANCELED') return;
      set({ directSalesError: true, directSalesLoading: false });
    }
  }
})));
