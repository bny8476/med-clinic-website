import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { axiosPrivate } from '../../api/axios';
import { toast } from 'react-hot-toast';
import { 
  Pill, 
  CheckCircle2, 
  XCircle, 
  Clock, 
  Search, 
  Filter, 
  User, 
  FileText, 
  Calendar,
  AlertCircle,
  RefreshCw,
  Eye,
  UserCheck,
  Play,
  PackageCheck
} from 'lucide-react';
import Modal from '../../components/ui/Modal';
import Button from '../../components/ui/Button';

const PharmacistDashboard = () => {
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState('PENDING'); // 'PENDING', 'ACCEPTED', 'PROCESSING', 'DISPENSED', 'ALL'
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedPrescription, setSelectedPrescription] = useState(null);
  const [isDetailModalOpen, setIsDetailModalOpen] = useState(false);
  const [rejectReason, setRejectReason] = useState('');
  const [showRejectInput, setShowRejectInput] = useState(false);

  // Fetch pending/processed prescriptions for pharmacy queue
  const { data: prescriptions = [], isLoading, refetch, isRefetching } = useQuery({
    queryKey: ['pharmacyPendingPrescriptions'],
    queryFn: async () => {
      const res = await axiosPrivate.get('/prescriptions/pharmacy/queue');
      return res.data || [];
    },
    refetchInterval: 10000 // Auto refresh every 10 seconds
  });

  // Claim mutation (Atomic Concurrency lock)
  const claimMutation = useMutation({
    mutationFn: async (id) => axiosPrivate.post(`/prescriptions/${id}/claim`),
    onSuccess: (res) => {
      toast.success('Prescription claimed successfully');
      queryClient.invalidateQueries(['pharmacyPendingPrescriptions']);
      if (selectedPrescription?.id === res.data.id) {
        setSelectedPrescription(res.data);
      }
    },
    onError: (err) => {
      if (err.response?.status === 409) {
        toast.error('This prescription has already been claimed by another pharmacist.', { duration: 5000 });
      } else {
        toast.error(err.response?.data?.message || 'Failed to claim prescription');
      }
      queryClient.invalidateQueries(['pharmacyPendingPrescriptions']);
    }
  });

  // Start Processing mutation
  const processingMutation = useMutation({
    mutationFn: async (id) => axiosPrivate.post(`/prescriptions/${id}/processing`),
    onSuccess: (res) => {
      toast.success('Prescription processing started');
      queryClient.invalidateQueries(['pharmacyPendingPrescriptions']);
      if (selectedPrescription?.id === res.data.id) {
        setSelectedPrescription(res.data);
      }
    },
    onError: (err) => {
      toast.error(err.response?.data?.message || 'Failed to start processing');
    }
  });

  // Dispense mutation
  const dispenseMutation = useMutation({
    mutationFn: async (id) => axiosPrivate.post(`/prescriptions/${id}/dispense`),
    onSuccess: (res) => {
      toast.success('Prescription marked as Dispensed');
      queryClient.invalidateQueries(['pharmacyPendingPrescriptions']);
      setIsDetailModalOpen(false);
    },
    onError: (err) => {
      toast.error(err.response?.data?.message || 'Failed to dispense prescription');
    }
  });

  // Reject mutation
  const rejectMutation = useMutation({
    mutationFn: async ({ id, reason }) => axiosPrivate.post(`/prescriptions/${id}/reject`, { reason }),
    onSuccess: () => {
      toast.success('Prescription rejected');
      queryClient.invalidateQueries(['pharmacyPendingPrescriptions']);
      setIsDetailModalOpen(false);
      setShowRejectInput(false);
      setRejectReason('');
    },
    onError: (err) => {
      toast.error(err.response?.data?.message || 'Failed to reject prescription');
    }
  });

  // Filter logic
  const filteredPrescriptions = prescriptions.filter(p => {
    let statusMatch = true;
    if (activeTab === 'PENDING') statusMatch = p.pharmacyStatus === 'PENDING';
    else if (activeTab === 'IN_PROGRESS') statusMatch = p.pharmacyStatus === 'ACCEPTED' || p.pharmacyStatus === 'PROCESSING';
    else if (activeTab === 'DISPENSED') statusMatch = p.pharmacyStatus === 'DISPENSED';
    else if (activeTab !== 'ALL') statusMatch = p.pharmacyStatus === activeTab;

    const searchMatch = !searchTerm || 
      (p.patientName && p.patientName.toLowerCase().includes(searchTerm.toLowerCase())) ||
      (p.doctorName && p.doctorName.toLowerCase().includes(searchTerm.toLowerCase())) ||
      (p.id && p.id.toString().includes(searchTerm));
    return statusMatch && searchMatch;
  });

  const pendingCount = prescriptions.filter(p => p.pharmacyStatus === 'PENDING').length;
  const inProgressCount = prescriptions.filter(p => p.pharmacyStatus === 'ACCEPTED' || p.pharmacyStatus === 'PROCESSING').length;
  const dispensedCount = prescriptions.filter(p => p.pharmacyStatus === 'DISPENSED').length;

  const handleOpenDetail = (prescription) => {
    setSelectedPrescription(prescription);
    setShowRejectInput(false);
    setRejectReason('');
    setIsDetailModalOpen(true);
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'ACCEPTED':
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 bg-blue-50 text-blue-700 rounded-full text-xs font-bold border border-blue-200">
            <UserCheck className="w-3.5 h-3.5" /> Claimed / Accepted
          </span>
        );
      case 'PROCESSING':
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 bg-purple-50 text-purple-700 rounded-full text-xs font-bold border border-purple-200 animate-pulse">
            <Play className="w-3.5 h-3.5" /> Processing Order
          </span>
        );
      case 'DISPENSED':
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 bg-emerald-50 text-emerald-700 rounded-full text-xs font-bold border border-emerald-200">
            <CheckCircle2 className="w-3.5 h-3.5" /> Dispensed
          </span>
        );
      case 'REJECTED':
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 bg-rose-50 text-rose-700 rounded-full text-xs font-bold border border-rose-200">
            <XCircle className="w-3.5 h-3.5" /> Rejected
          </span>
        );
      case 'CANCELLED':
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 bg-slate-100 text-slate-700 rounded-full text-xs font-bold border border-slate-200">
            <XCircle className="w-3.5 h-3.5" /> Cancelled
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 bg-amber-50 text-amber-700 rounded-full text-xs font-bold border border-amber-200 animate-pulse">
            <Clock className="w-3.5 h-3.5" /> Pending Fulfillment
          </span>
        );
    }
  };

  return (
    <div className="p-6 max-w-7xl mx-auto space-y-6">
      
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-white p-6 rounded-2xl border border-slate-200/80 shadow-2xs">
        <div>
          <div className="flex items-center gap-3">
            <div className="p-3 bg-teal-50 text-teal-600 rounded-xl">
              <Pill className="w-6 h-6" />
            </div>
            <div>
              <h1 className="text-2xl font-extrabold text-slate-900">Pharmacist Prescription Queue</h1>
              <p className="text-xs text-slate-500 font-medium mt-0.5">
                Claim, process, and dispense incoming doctor e-prescriptions
              </p>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={() => refetch()}
            disabled={isRefetching}
            className="flex items-center gap-2 px-4 py-2.5 bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold text-xs rounded-xl transition"
          >
            <RefreshCw className={`w-4 h-4 ${isRefetching ? 'animate-spin' : ''}`} />
            Refresh Queue
          </button>
        </div>
      </div>

      {/* Metric Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div 
          onClick={() => setActiveTab('PENDING')}
          className={`p-5 rounded-2xl border cursor-pointer transition ${activeTab === 'PENDING' ? 'bg-amber-50/50 border-amber-300 ring-2 ring-amber-400/20' : 'bg-white border-slate-200/80 hover:border-amber-200'}`}
        >
          <div className="flex justify-between items-center">
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">Unclaimed Orders</span>
            <Clock className="w-5 h-5 text-amber-500" />
          </div>
          <p className="text-3xl font-black text-slate-900 mt-2">{pendingCount}</p>
        </div>

        <div 
          onClick={() => setActiveTab('IN_PROGRESS')}
          className={`p-5 rounded-2xl border cursor-pointer transition ${activeTab === 'IN_PROGRESS' ? 'bg-blue-50/50 border-blue-300 ring-2 ring-blue-400/20' : 'bg-white border-slate-200/80 hover:border-blue-200'}`}
        >
          <div className="flex justify-between items-center">
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">In Progress</span>
            <UserCheck className="w-5 h-5 text-blue-500" />
          </div>
          <p className="text-3xl font-black text-slate-900 mt-2">{inProgressCount}</p>
        </div>

        <div 
          onClick={() => setActiveTab('DISPENSED')}
          className={`p-5 rounded-2xl border cursor-pointer transition ${activeTab === 'DISPENSED' ? 'bg-emerald-50/50 border-emerald-300 ring-2 ring-emerald-400/20' : 'bg-white border-slate-200/80 hover:border-emerald-200'}`}
        >
          <div className="flex justify-between items-center">
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">Dispensed</span>
            <CheckCircle2 className="w-5 h-5 text-emerald-500" />
          </div>
          <p className="text-3xl font-black text-slate-900 mt-2">{dispensedCount}</p>
        </div>

        <div 
          onClick={() => setActiveTab('ALL')}
          className={`p-5 rounded-2xl border cursor-pointer transition ${activeTab === 'ALL' ? 'bg-indigo-50/50 border-indigo-300 ring-2 ring-indigo-400/20' : 'bg-white border-slate-200/80 hover:border-indigo-200'}`}
        >
          <div className="flex justify-between items-center">
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">Total Queue</span>
            <FileText className="w-5 h-5 text-indigo-500" />
          </div>
          <p className="text-3xl font-black text-slate-900 mt-2">{prescriptions.length}</p>
        </div>
      </div>

      {/* Main Inbox Table Container */}
      <div className="bg-white rounded-2xl border border-slate-200/80 shadow-2xs overflow-hidden">
        
        {/* Controls Bar */}
        <div className="p-4 border-b border-slate-100 flex flex-col md:flex-row md:items-center justify-between gap-4 bg-slate-50/50">
          
          {/* Tab Navigation */}
          <div className="flex items-center gap-1 bg-slate-200/60 p-1 rounded-xl">
            <button
              onClick={() => setActiveTab('PENDING')}
              className={`px-4 py-1.5 text-xs font-bold rounded-lg transition ${activeTab === 'PENDING' ? 'bg-white text-slate-900 shadow-2xs' : 'text-slate-600 hover:text-slate-900'}`}
            >
              Unclaimed ({pendingCount})
            </button>
            <button
              onClick={() => setActiveTab('IN_PROGRESS')}
              className={`px-4 py-1.5 text-xs font-bold rounded-lg transition ${activeTab === 'IN_PROGRESS' ? 'bg-white text-slate-900 shadow-2xs' : 'text-slate-600 hover:text-slate-900'}`}
            >
              In Progress ({inProgressCount})
            </button>
            <button
              onClick={() => setActiveTab('DISPENSED')}
              className={`px-4 py-1.5 text-xs font-bold rounded-lg transition ${activeTab === 'DISPENSED' ? 'bg-white text-slate-900 shadow-2xs' : 'text-slate-600 hover:text-slate-900'}`}
            >
              Dispensed ({dispensedCount})
            </button>
            <button
              onClick={() => setActiveTab('ALL')}
              className={`px-4 py-1.5 text-xs font-bold rounded-lg transition ${activeTab === 'ALL' ? 'bg-white text-slate-900 shadow-2xs' : 'text-slate-600 hover:text-slate-900'}`}
            >
              All Prescriptions
            </button>
          </div>

          {/* Search Box */}
          <div className="relative w-full md:w-72">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-3" />
            <input
              type="text"
              placeholder="Search Rx ID, patient, doctor..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-9 pr-4 py-2 bg-white border border-slate-200 rounded-xl text-xs font-medium focus:outline-none focus:border-teal-500 focus:ring-2 focus:ring-teal-500/20"
            />
          </div>
        </div>

        {/* Table View */}
        {isLoading ? (
          <div className="p-12 text-center text-slate-400 text-xs font-medium">
            Loading prescription queue...
          </div>
        ) : filteredPrescriptions.length === 0 ? (
          <div className="p-12 text-center space-y-3">
            <div className="w-12 h-12 bg-slate-100 text-slate-400 rounded-full flex items-center justify-center mx-auto">
              <FileText className="w-6 h-6" />
            </div>
            <p className="text-slate-600 font-bold text-sm">No prescriptions found</p>
            <p className="text-slate-400 text-xs">No matching orders in the queue right now.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-slate-100 bg-slate-50/50 text-[11px] font-extrabold uppercase text-slate-400 tracking-wider">
                  <th className="py-3 px-4">Rx ID</th>
                  <th className="py-3 px-4">Patient</th>
                  <th className="py-3 px-4">Doctor</th>
                  <th className="py-3 px-4">Items</th>
                  <th className="py-3 px-4">Date</th>
                  <th className="py-3 px-4">Fulfillment Status</th>
                  <th className="py-3 px-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-xs font-medium text-slate-700">
                {filteredPrescriptions.map((rx) => (
                  <tr key={rx.id} className="hover:bg-slate-50/80 transition-colors">
                    <td className="py-3.5 px-4 font-bold text-slate-900">#{rx.id}</td>
                    <td className="py-3.5 px-4">
                      <div className="font-bold text-slate-900">{rx.patientName || `Patient #${rx.patientId}`}</div>
                    </td>
                    <td className="py-3.5 px-4 text-slate-600">
                      {rx.doctorName || `Doctor #${rx.doctorId}`}
                    </td>
                    <td className="py-3.5 px-4">
                      <span className="font-bold text-teal-700 bg-teal-50 px-2.5 py-1 rounded-md border border-teal-100">
                        {rx.items ? rx.items.length : 0} items
                      </span>
                    </td>
                    <td className="py-3.5 px-4 text-slate-500">
                      {rx.createdAt ? new Date(rx.createdAt).toLocaleDateString() : 'N/A'}
                    </td>
                    <td className="py-3.5 px-4">
                      {getStatusBadge(rx.pharmacyStatus)}
                    </td>
                    <td className="py-3.5 px-4 text-right space-x-2">
                      {rx.pharmacyStatus === 'PENDING' && (
                        <button
                          onClick={() => claimMutation.mutate(rx.id)}
                          disabled={claimMutation.isPending}
                          className="px-3 py-1.5 bg-teal-600 hover:bg-teal-700 text-white font-bold rounded-lg transition inline-flex items-center gap-1.5 shadow-2xs"
                        >
                          <UserCheck className="w-3.5 h-3.5" /> Claim
                        </button>
                      )}

                      {rx.pharmacyStatus === 'ACCEPTED' && (
                        <button
                          onClick={() => processingMutation.mutate(rx.id)}
                          disabled={processingMutation.isPending}
                          className="px-3 py-1.5 bg-blue-600 hover:bg-blue-700 text-white font-bold rounded-lg transition inline-flex items-center gap-1.5 shadow-2xs"
                        >
                          <Play className="w-3.5 h-3.5" /> Start Processing
                        </button>
                      )}

                      {rx.pharmacyStatus === 'PROCESSING' && (
                        <button
                          onClick={() => dispenseMutation.mutate(rx.id)}
                          disabled={dispenseMutation.isPending}
                          className="px-3 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white font-bold rounded-lg transition inline-flex items-center gap-1.5 shadow-2xs"
                        >
                          <PackageCheck className="w-3.5 h-3.5" /> Dispense
                        </button>
                      )}

                      <button
                        onClick={() => handleOpenDetail(rx)}
                        className="px-3 py-1.5 bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold rounded-lg transition inline-flex items-center gap-1.5"
                      >
                        <Eye className="w-3.5 h-3.5" /> View Details
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Prescription Detail & State Control Modal */}
      {isDetailModalOpen && selectedPrescription && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm p-4">
          <div className="bg-white rounded-2xl shadow-xl w-full max-w-2xl overflow-hidden flex flex-col">
            
            {/* Modal Header */}
            <div className="px-6 py-4 border-b border-slate-100 bg-slate-50 flex justify-between items-center">
              <div>
                <h3 className="font-bold text-slate-900 text-base flex items-center gap-2">
                  <Pill className="w-5 h-5 text-teal-600" />
                  Prescription Order #{selectedPrescription.id}
                </h3>
                <p className="text-xs text-slate-500 mt-0.5">
                  Patient: <strong className="text-slate-800">{selectedPrescription.patientName || selectedPrescription.patientId}</strong>
                </p>
              </div>
              <div>
                {getStatusBadge(selectedPrescription.pharmacyStatus)}
              </div>
            </div>

            {/* Modal Body */}
            <div className="p-6 space-y-5 max-h-[70vh] overflow-y-auto">
              
              {/* Doctor & Diagnosis summary */}
              <div className="grid grid-cols-2 gap-4 p-4 bg-slate-50 rounded-xl border border-slate-100 text-xs">
                <div>
                  <span className="text-slate-400 font-bold block mb-1">Prescribing Doctor</span>
                  <span className="font-bold text-slate-900">{selectedPrescription.doctorName || `Doctor #${selectedPrescription.doctorId}`}</span>
                </div>
                <div>
                  <span className="text-slate-400 font-bold block mb-1">Diagnosis / Notes</span>
                  <span className="font-medium text-slate-800">{selectedPrescription.diagnosis || selectedPrescription.notes || 'N/A'}</span>
                </div>
              </div>

              {/* Items Table */}
              <div>
                <h4 className="text-xs font-extrabold uppercase tracking-wider text-slate-400 mb-3">Prescribed Medications</h4>
                <div className="border border-slate-200 rounded-xl overflow-hidden">
                  <table className="w-full text-left text-xs">
                    <thead className="bg-slate-50 text-slate-500 font-bold border-b border-slate-200">
                      <tr>
                        <th className="py-2.5 px-3">Medication</th>
                        <th className="py-2.5 px-3">Dosage</th>
                        <th className="py-2.5 px-3">Frequency</th>
                        <th className="py-2.5 px-3">Duration</th>
                        <th className="py-2.5 px-3">Instructions</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100 font-medium text-slate-800">
                      {selectedPrescription.items && selectedPrescription.items.length > 0 ? (
                        selectedPrescription.items.map((item, idx) => (
                          <tr key={idx}>
                            <td className="py-2.5 px-3 font-bold text-teal-900">{item.medicationName}</td>
                            <td className="py-2.5 px-3">{item.dosage || 'N/A'}</td>
                            <td className="py-2.5 px-3">{item.frequency || 'N/A'}</td>
                            <td className="py-2.5 px-3">{item.duration || 'N/A'}</td>
                            <td className="py-2.5 px-3 text-slate-500">{item.instructions || 'Standard'}</td>
                          </tr>
                        ))
                      ) : (
                        <tr>
                          <td colSpan="5" className="py-4 text-center text-slate-400">No items specified</td>
                        </tr>
                      )}
                    </tbody>
                  </table>
                </div>
              </div>

              {/* Rejection input box if opened */}
              {showRejectInput && (
                <div className="p-4 bg-rose-50 border border-rose-200 rounded-xl space-y-3">
                  <label className="block text-xs font-bold text-rose-900">Reason for Rejection</label>
                  <textarea
                    value={rejectReason}
                    onChange={(e) => setRejectReason(e.target.value)}
                    placeholder="Enter clear rejection reason for doctor..."
                    className="w-full p-2.5 bg-white border border-rose-300 rounded-lg text-xs focus:outline-none focus:ring-2 focus:ring-rose-500/20"
                    rows={2}
                  />
                  <div className="flex justify-end gap-2">
                    <button
                      onClick={() => setShowRejectInput(false)}
                      className="px-3 py-1.5 text-xs font-bold text-slate-600 hover:bg-slate-200 rounded-lg"
                    >
                      Cancel
                    </button>
                    <button
                      onClick={() => rejectMutation.mutate({ id: selectedPrescription.id, reason: rejectReason })}
                      disabled={rejectMutation.isPending}
                      className="px-4 py-1.5 text-xs font-bold text-white bg-rose-600 hover:bg-rose-700 rounded-lg shadow-2xs"
                    >
                      Confirm Reject
                    </button>
                  </div>
                </div>
              )}

            </div>

            {/* Modal Footer Actions */}
            <div className="p-4 border-t border-slate-100 bg-slate-50 flex justify-between items-center">
              <button
                onClick={() => setIsDetailModalOpen(false)}
                className="px-4 py-2 text-xs font-bold text-slate-600 hover:bg-slate-200 rounded-xl transition"
              >
                Close
              </button>

              <div className="flex items-center gap-2">
                {selectedPrescription.pharmacyStatus === 'PENDING' && (
                  <button
                    onClick={() => claimMutation.mutate(selectedPrescription.id)}
                    disabled={claimMutation.isPending}
                    className="px-5 py-2 text-xs font-bold text-white bg-teal-600 hover:bg-teal-700 rounded-xl shadow-md transition flex items-center gap-1.5"
                  >
                    <UserCheck className="w-4 h-4" />
                    Claim Order
                  </button>
                )}

                {selectedPrescription.pharmacyStatus === 'ACCEPTED' && (
                  <>
                    <button
                      onClick={() => setShowRejectInput(true)}
                      className="px-4 py-2 text-xs font-bold text-rose-700 bg-rose-50 hover:bg-rose-100 rounded-xl border border-rose-200 transition"
                    >
                      Reject Order
                    </button>
                    <button
                      onClick={() => processingMutation.mutate(selectedPrescription.id)}
                      disabled={processingMutation.isPending}
                      className="px-5 py-2 text-xs font-bold text-white bg-blue-600 hover:bg-blue-700 rounded-xl shadow-md transition flex items-center gap-1.5"
                    >
                      <Play className="w-4 h-4" />
                      Start Processing
                    </button>
                  </>
                )}

                {selectedPrescription.pharmacyStatus === 'PROCESSING' && (
                  <>
                    <button
                      onClick={() => setShowRejectInput(true)}
                      className="px-4 py-2 text-xs font-bold text-rose-700 bg-rose-50 hover:bg-rose-100 rounded-xl border border-rose-200 transition"
                    >
                      Reject Order
                    </button>
                    <button
                      onClick={() => dispenseMutation.mutate(selectedPrescription.id)}
                      disabled={dispenseMutation.isPending}
                      className="px-5 py-2 text-xs font-bold text-white bg-emerald-600 hover:bg-emerald-700 rounded-xl shadow-md transition flex items-center gap-1.5"
                    >
                      <PackageCheck className="w-4 h-4" />
                      Mark as Dispensed
                    </button>
                  </>
                )}
              </div>
            </div>

          </div>
        </div>
      )}

    </div>
  );
};

export default PharmacistDashboard;
