import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { axiosPrivate } from '../../api/axios';
import { toast } from 'react-hot-toast';
import { 
  RefreshCw, 
  AlertTriangle, 
  CheckCircle2, 
  Clock, 
  ExternalLink, 
  Search,
  Server,
  FileText,
  ShieldCheck,
  Send
} from 'lucide-react';

const PharmacySyncAdminDashboard = () => {
  const queryClient = useQueryClient();
  const [searchTerm, setSearchTerm] = useState('');
  const [activeTab, setActiveTab] = useState('FAILED'); // 'FAILED', 'ALL'

  // Fetch failed or pending sync prescriptions
  const { data: failedPrescriptions = [], isLoading, refetch, isRefetching } = useQuery({
    queryKey: ['failedPharmacySyncPrescriptions'],
    queryFn: async () => {
      const res = await axiosPrivate.get('/integrations/pharmacy/prescriptions/failed-sync');
      return res.data || [];
    },
    refetchInterval: 15000
  });

  // Manual retry sync mutation
  const retryMutation = useMutation({
    mutationFn: async (id) => axiosPrivate.post(`/integrations/pharmacy/prescriptions/${id}/retry-sync`),
    onSuccess: (res) => {
      toast.success(`Prescription #${res.data.id} sync retried successfully!`);
      queryClient.invalidateQueries(['failedPharmacySyncPrescriptions']);
    },
    onError: (err) => {
      toast.error(err.response?.data?.message || 'Manual retry failed. External service may still be down.');
    }
  });

  const filteredPrescriptions = failedPrescriptions.filter(p => {
    const searchMatch = !searchTerm || 
      (p.id && p.id.toString().includes(searchTerm)) ||
      (p.pharmacyReferenceId && p.pharmacyReferenceId.toLowerCase().includes(searchTerm.toLowerCase())) ||
      (p.pharmacySyncError && p.pharmacySyncError.toLowerCase().includes(searchTerm.toLowerCase()));
    return searchMatch;
  });

  return (
    <div className="p-6 max-w-7xl mx-auto space-y-6">
      
      {/* Header Banner */}
      <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-2xs flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div className="flex items-center gap-4">
          <div className="p-3 bg-teal-50 text-teal-600 rounded-xl">
            <Server className="w-7 h-7" />
          </div>
          <div>
            <h1 className="text-2xl font-black text-slate-900">External Pharmacy Integration Monitor</h1>
            <p className="text-xs text-slate-500 font-medium mt-0.5">
              Service Endpoint: <code className="bg-slate-100 text-teal-700 font-mono px-2 py-0.5 rounded text-[11px]">https://pms-pharmadesk.onrender.com</code>
            </p>
          </div>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={() => refetch()}
            disabled={isRefetching}
            className="flex items-center gap-2 px-4 py-2.5 bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold text-xs rounded-xl transition"
          >
            <RefreshCw className={`w-4 h-4 ${isRefetching ? 'animate-spin' : ''}`} />
            Refresh Monitor
          </button>
        </div>
      </div>

      {/* Integration Status Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="bg-amber-50/60 p-5 rounded-2xl border border-amber-200">
          <div className="flex justify-between items-center text-amber-700">
            <span className="text-xs font-bold uppercase tracking-wider">Retry Pending Syncs</span>
            <Clock className="w-5 h-5" />
          </div>
          <p className="text-3xl font-black text-amber-900 mt-2">{failedPrescriptions.length}</p>
        </div>

        <div className="bg-blue-50/60 p-5 rounded-2xl border border-blue-200">
          <div className="flex justify-between items-center text-blue-700">
            <span className="text-xs font-bold uppercase tracking-wider">Auto Outbox Retry Worker</span>
            <ShieldCheck className="w-5 h-5" />
          </div>
          <p className="text-xs font-bold text-blue-900 mt-2">Active (@Scheduled every 2m)</p>
        </div>

        <div className="bg-emerald-50/60 p-5 rounded-2xl border border-emerald-200">
          <div className="flex justify-between items-center text-emerald-700">
            <span className="text-xs font-bold uppercase tracking-wider">Status Webhook Callback</span>
            <ExternalLink className="w-5 h-5" />
          </div>
          <p className="text-xs font-bold text-emerald-900 mt-2">Listening on /api/integrations/pharmacy/webhooks</p>
        </div>
      </div>

      {/* Main Table */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-2xs overflow-hidden">
        
        {/* Controls */}
        <div className="p-4 border-b border-slate-100 flex flex-col md:flex-row md:items-center justify-between gap-4 bg-slate-50/50">
          <div className="flex items-center gap-2">
            <span className="text-xs font-extrabold text-slate-700 uppercase tracking-wider">Sync Log Queue</span>
          </div>

          <div className="relative w-full md:w-72">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-3" />
            <input
              type="text"
              placeholder="Search Rx ID, Ref ID, error..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-9 pr-4 py-2 bg-white border border-slate-200 rounded-xl text-xs font-medium focus:outline-none focus:border-teal-500"
            />
          </div>
        </div>

        {/* Table Content */}
        {isLoading ? (
          <div className="p-12 text-center text-slate-400 text-xs font-medium">
            Loading external pharmacy sync logs...
          </div>
        ) : filteredPrescriptions.length === 0 ? (
          <div className="p-12 text-center space-y-2">
            <CheckCircle2 className="w-10 h-10 text-emerald-500 mx-auto" />
            <p className="text-slate-800 font-bold text-sm">No Pending Sync Errors</p>
            <p className="text-slate-400 text-xs">All external pharmacy submissions are in sync.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-slate-100 bg-slate-50/50 text-[11px] font-extrabold uppercase text-slate-400 tracking-wider">
                  <th className="py-3 px-4">Rx ID</th>
                  <th className="py-3 px-4">External Ref ID</th>
                  <th className="py-3 px-4">Fulfillment Status</th>
                  <th className="py-3 px-4">Last Sync Error / Diagnostic</th>
                  <th className="py-3 px-4">Sent At</th>
                  <th className="py-3 px-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-xs font-medium text-slate-700">
                {filteredPrescriptions.map((rx) => (
                  <tr key={rx.id} className="hover:bg-slate-50/80 transition-colors">
                    <td className="py-3.5 px-4 font-bold text-slate-900">#{rx.id}</td>
                    <td className="py-3.5 px-4 font-mono text-[11px] text-teal-800 font-bold">
                      {rx.pharmacyReferenceId || 'N/A'}
                    </td>
                    <td className="py-3.5 px-4">
                      <span className="inline-flex items-center gap-1.5 px-2.5 py-1 bg-amber-50 text-amber-800 rounded-full text-xs font-bold border border-amber-200">
                        <Clock className="w-3.5 h-3.5 text-amber-600" />
                        {rx.pharmacyStatus || 'RETRY_PENDING'}
                      </span>
                    </td>
                    <td className="py-3.5 px-4 text-slate-600 max-w-xs truncate">
                      <span className="text-rose-700 bg-rose-50 px-2 py-0.5 rounded text-[11px] font-medium border border-rose-100">
                        {rx.pharmacySyncError || 'Service timeout / 503 response'}
                      </span>
                    </td>
                    <td className="py-3.5 px-4 text-slate-400 text-[11px]">
                      {rx.sentToPharmacyAt ? new Date(rx.sentToPharmacyAt).toLocaleString() : 'N/A'}
                    </td>
                    <td className="py-3.5 px-4 text-right">
                      <button
                        onClick={() => retryMutation.mutate(rx.id)}
                        disabled={retryMutation.isPending}
                        className="px-3.5 py-1.5 bg-teal-600 hover:bg-teal-700 text-white font-bold rounded-lg transition inline-flex items-center gap-1.5 shadow-2xs"
                      >
                        <Send className="w-3.5 h-3.5" /> Retry Sync
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

    </div>
  );
};

export default PharmacySyncAdminDashboard;
