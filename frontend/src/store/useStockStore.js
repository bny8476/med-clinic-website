import { create } from 'zustand';
import pharmacyService from '../utils/pharmacy/pharmacyService';
import { toast } from 'react-hot-toast';

export const useStockStore = create((set, get) => ({
  // Medicine Stock State
  stocks: [],
  medicines: [],
  valuation: null,
  suppliers: [],
  stockLoading: false,

  fetchStocks: async () => {
    set({ stockLoading: true });
    try {
      const [stockRes, medRes, valRes, suppRes] = await Promise.all([
        pharmacyService.getAllStocks(),
        pharmacyService.getMedicines(),
        pharmacyService.api.get('/pharmacy/stocks/valuation').catch(() => null),
        pharmacyService.getSuppliers().catch(() => ({ data: [] }))
      ]);

      const extractArray = (res) => {
        if (!res) return [];
        const d = res.data;
        if (Array.isArray(d)) return d;
        if (d && Array.isArray(d.content)) return d.content;
        return [];
      };

      set({
        stocks: extractArray(stockRes),
        medicines: extractArray(medRes),
        valuation: valRes?.data?.success ? valRes.data.data : null,
        suppliers: extractArray(suppRes),
        stockLoading: false
      });
    } catch (error) {
      toast.error('Failed to load stock data');
      set({ stockLoading: false });
    }
  },

  adjustStock: async (stockId, adjustedQuantity, reason, remarks) => {
    try {
      await pharmacyService.api.post('/pharmacy/stocks/adjust', {
        medicineStock: { id: stockId },
        adjustedQuantity,
        reason,
        remarks
      });
      toast.success('Stock adjusted successfully');
      get().fetchStocks();
      return true;
    } catch (error) {
      toast.error('Adjustment failed');
      return false;
    }
  },

  addStock: async (payload) => {
    try {
      await pharmacyService.api.post('/pharmacy/stocks', payload);
      toast.success('Stock inward registered successfully');
      get().fetchStocks();
      return true;
    } catch (error) {
      toast.error(error.response?.data?.message || 'Failed to add stock');
      return false;
    }
  },

  updateReorderConfig: async (id, payload) => {
    try {
      await pharmacyService.api.put(`/pharmacy/medicines/${id}`, payload);
      toast.success('Reorder settings updated successfully');
      get().fetchStocks();
      return true;
    } catch (error) {
      toast.error(error.response?.data?.message || 'Failed to update reorder settings');
      return false;
    }
  },

  runAutoPO: async () => {
    try {
      const res = await pharmacyService.api.post('/pharmacy/purchase-orders/auto-generate');
      toast.success(res.data.message || 'Auto POs generated');
    } catch (e) {
      toast.error('Failed to generate POs');
    }
  },

  // Low Stock Alerts State
  lowStockItems: [],
  lowStockPage: 0,
  lowStockTotalElements: 0,
  lowStockLoading: false,
  lowStockError: false,
  lowStockFetching: false,

  setLowStockPage: (page) => {
    set({ lowStockPage: page });
    get().fetchLowStockAlerts();
  },

  fetchLowStockAlerts: async (showBackgroundFetch = false) => {
    set({ [showBackgroundFetch ? 'lowStockFetching' : 'lowStockLoading']: true, lowStockError: false });
    try {
      const params = {
        page: get().lowStockPage,
        size: 20
      };
      const res = await pharmacyService.api.get('/pharmacy/stocks/low-stock', { params });
      
      set({
        lowStockItems: res.data?.data?.content || [],
        lowStockTotalElements: res.data?.data?.totalElements || 0,
        lowStockLoading: false,
        lowStockFetching: false
      });
    } catch (error) {
      set({ lowStockError: true, lowStockLoading: false, lowStockFetching: false });
    }
  }
}));
