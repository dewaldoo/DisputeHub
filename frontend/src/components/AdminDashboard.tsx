import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '../contexts/AuthContext';
import { apiService } from '../services/api';
import { Dispute, DisputeStatus, UpdateDisputeStatusRequest, ApiError } from '../types';

export const AdminDashboard: React.FC = () => {
  const { user, logout } = useAuth();
  const queryClient = useQueryClient();
  const [selectedDispute, setSelectedDispute] = useState<Dispute | null>(null);
  const [showStatusModal, setShowStatusModal] = useState(false);
  const [newStatus, setNewStatus] = useState<DisputeStatus>(DisputeStatus.PENDING);
  const [error, setError] = useState('');
  const [filterStatus, setFilterStatus] = useState<string>('ALL');
  const [currentPage, setCurrentPage] = useState(0);
  const pageSize = 20;

  // Fetch all disputes (paginated)
  const { data: disputesPage, isLoading } = useQuery({
    queryKey: ['allDisputes', currentPage, pageSize],
    queryFn: () => apiService.getAllDisputes(currentPage, pageSize),
  });

  const disputes = disputesPage?.content || [];

  // Update dispute status mutation
  const updateStatusMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: UpdateDisputeStatusRequest }) =>
      apiService.updateDisputeStatus(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['allDisputes'] });
      setShowStatusModal(false);
      setSelectedDispute(null);
      setError('');
    },
    onError: (err: ApiError) => {
      setError(err.message || 'Failed to update dispute status');
    },
  });

  const handleUpdateStatus = () => {
    if (!selectedDispute) return;
    setError('');

    updateStatusMutation.mutate({
      id: selectedDispute.id,
      data: { status: newStatus },
    });
  };

  const openStatusModal = (dispute: Dispute) => {
    setSelectedDispute(dispute);
    setNewStatus(dispute.status);
    setShowStatusModal(true);
    setError('');
  };

  const closeStatusModal = () => {
    setShowStatusModal(false);
    setSelectedDispute(null);
    setError('');
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-ZA', {
      style: 'currency',
      currency: 'ZAR',
    }).format(amount);
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-ZA', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const getStatusColor = (status: string) => {
    switch (status.toUpperCase()) {
      case 'PENDING':
        return 'bg-yellow-100 text-yellow-800';
      case 'UNDER_REVIEW':
        return 'bg-blue-100 text-blue-800';
      case 'MERCHANT_CONTACTED':
        return 'bg-purple-100 text-purple-800';
      case 'RESOLVED':
        return 'bg-green-100 text-green-800';
      case 'REJECTED':
        return 'bg-red-100 text-red-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  const filteredDisputes = disputes?.filter((dispute) => {
    if (filterStatus === 'ALL') return true;
    return dispute.status === filterStatus;
  });

  const getStatusCount = (status: string) => {
    if (!disputes) return 0;
    return disputes.filter((d) => d.status === status).length;
  };

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white shadow">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
          <div className="flex justify-between items-center">
            <div>
              <h1 className="text-2xl font-bold text-gray-900">DisputeHub Admin</h1>
              <p className="text-sm text-gray-600 mt-1">Welcome, {user?.fullName}</p>
            </div>
            <button
              onClick={logout}
              className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500"
            >
              Sign Out
            </button>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Stats Cards */}
        <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-5 mb-8">
          <div className="bg-white overflow-hidden shadow rounded-lg">
            <div className="p-5">
              <div className="flex items-center">
                <div className="flex-1">
                  <dt className="text-sm font-medium text-gray-500 truncate">Total Disputes</dt>
                  <dd className="mt-1 text-3xl font-semibold text-gray-900">
                    {disputes?.length || 0}
                  </dd>
                </div>
              </div>
            </div>
          </div>

          <div className="bg-white overflow-hidden shadow rounded-lg">
            <div className="p-5">
              <div className="flex items-center">
                <div className="flex-1">
                  <dt className="text-sm font-medium text-gray-500 truncate">Pending</dt>
                  <dd className="mt-1 text-3xl font-semibold text-yellow-600">
                    {getStatusCount(DisputeStatus.PENDING)}
                  </dd>
                </div>
              </div>
            </div>
          </div>

          <div className="bg-white overflow-hidden shadow rounded-lg">
            <div className="p-5">
              <div className="flex items-center">
                <div className="flex-1">
                  <dt className="text-sm font-medium text-gray-500 truncate">Under Review</dt>
                  <dd className="mt-1 text-3xl font-semibold text-blue-600">
                    {getStatusCount(DisputeStatus.UNDER_REVIEW)}
                  </dd>
                </div>
              </div>
            </div>
          </div>

          <div className="bg-white overflow-hidden shadow rounded-lg">
            <div className="p-5">
              <div className="flex items-center">
                <div className="flex-1">
                  <dt className="text-sm font-medium text-gray-500 truncate">Resolved</dt>
                  <dd className="mt-1 text-3xl font-semibold text-green-600">
                    {getStatusCount(DisputeStatus.RESOLVED)}
                  </dd>
                </div>
              </div>
            </div>
          </div>

          <div className="bg-white overflow-hidden shadow rounded-lg">
            <div className="p-5">
              <div className="flex items-center">
                <div className="flex-1">
                  <dt className="text-sm font-medium text-gray-500 truncate">Rejected</dt>
                  <dd className="mt-1 text-3xl font-semibold text-red-600">
                    {getStatusCount(DisputeStatus.REJECTED)}
                  </dd>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Filter Bar */}
        <div className="mb-6 bg-white shadow rounded-lg p-4">
          <div className="flex items-center space-x-4">
            <label className="text-sm font-medium text-gray-700">Filter by Status:</label>
            <select
              value={filterStatus}
              onChange={(e) => setFilterStatus(e.target.value)}
              className="px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
            >
              <option value="ALL">All Disputes</option>
              <option value={DisputeStatus.PENDING}>Pending</option>
              <option value={DisputeStatus.UNDER_REVIEW}>Under Review</option>
              <option value={DisputeStatus.MERCHANT_CONTACTED}>Merchant Contacted</option>
              <option value={DisputeStatus.RESOLVED}>Resolved</option>
              <option value={DisputeStatus.REJECTED}>Rejected</option>
            </select>
          </div>
        </div>

        {/* Disputes List */}
        <div>
          <h2 className="text-xl font-semibold text-gray-900 mb-4">All Disputes</h2>

          {isLoading ? (
            <div className="text-center py-12">
              <div className="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600"></div>
              <p className="mt-2 text-gray-600">Loading disputes...</p>
            </div>
          ) : filteredDisputes && filteredDisputes.length > 0 ? (
            <div className="space-y-4">
              {filteredDisputes.map((dispute) => (
                <div key={dispute.id} className="bg-white shadow rounded-lg p-6">
                  <div className="flex justify-between items-start mb-4">
                    <div>
                      <h3 className="text-lg font-medium text-gray-900">
                        Dispute #{dispute.id}
                      </h3>
                      <p className="text-sm text-gray-500 mt-1">
                        Customer: {dispute.customerName} ({dispute.customerEmail})
                      </p>
                    </div>
                    <div className="flex items-center space-x-3">
                      <span
                        className={`px-3 py-1 inline-flex text-xs leading-5 font-semibold rounded-full ${getStatusColor(
                          dispute.status
                        )}`}
                      >
                        {dispute.status.replace('_', ' ')}
                      </span>
                      <button
                        onClick={() => openStatusModal(dispute)}
                        className="px-3 py-1 text-sm font-medium text-indigo-600 hover:text-indigo-500 border border-indigo-600 rounded-md hover:bg-indigo-50"
                      >
                        Update Status
                      </button>
                    </div>
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-4 pb-4 border-b border-gray-200">
                    <div>
                      <p className="text-sm font-medium text-gray-500">Transaction ID</p>
                      <p className="text-sm text-gray-900 mt-1">
                        {dispute.transactionId}
                      </p>
                    </div>
                    <div>
                      <p className="text-sm font-medium text-gray-500">Merchant</p>
                      <p className="text-sm text-gray-900 mt-1">
                        {dispute.merchantName}
                      </p>
                    </div>
                    <div>
                      <p className="text-sm font-medium text-gray-500">Amount</p>
                      <p className="text-sm text-gray-900 mt-1">
                        {formatCurrency(dispute.amount)}
                      </p>
                    </div>
                    <div>
                      <p className="text-sm font-medium text-gray-500">Transaction Date</p>
                      <p className="text-sm text-gray-900 mt-1">
                        {formatDate(dispute.transactionDate)}
                      </p>
                    </div>
                    <div>
                      <p className="text-sm font-medium text-gray-500">Created</p>
                      <p className="text-sm text-gray-900 mt-1">
                        {formatDate(dispute.createdAt)}
                      </p>
                    </div>
                    <div>
                      <p className="text-sm font-medium text-gray-500">Last Updated</p>
                      <p className="text-sm text-gray-900 mt-1">
                        {formatDate(dispute.updatedAt)}
                      </p>
                    </div>
                  </div>

                  <div className="mb-4">
                    <p className="text-sm font-medium text-gray-500">Reason</p>
                    <p className="text-sm text-gray-900 mt-1">{dispute.reason}</p>
                  </div>

                  <div>
                    <p className="text-sm font-medium text-gray-500">Description</p>
                    <p className="text-sm text-gray-900 mt-1">{dispute.description}</p>
                  </div>
                </div>
              ))}

            {/* Pagination Controls */}
            {disputesPage && disputesPage.totalPages > 1 && (
              <div className="bg-white px-4 py-3 flex items-center justify-between border-t border-gray-200 sm:px-6 mt-4 rounded-lg shadow">
                <div className="flex-1 flex justify-between sm:hidden">
                  <button
                    onClick={() => setCurrentPage(Math.max(0, currentPage - 1))}
                    disabled={currentPage === 0}
                    className="relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    Previous
                  </button>
                  <button
                    onClick={() => setCurrentPage(Math.min(disputesPage.totalPages - 1, currentPage + 1))}
                    disabled={currentPage === disputesPage.totalPages - 1}
                    className="ml-3 relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    Next
                  </button>
                </div>
                <div className="hidden sm:flex-1 sm:flex sm:items-center sm:justify-between">
                  <div>
                    <p className="text-sm text-gray-700">
                      Showing page <span className="font-medium">{currentPage + 1}</span> of{' '}
                      <span className="font-medium">{disputesPage.totalPages}</span> (
                      <span className="font-medium">{disputesPage.totalElements}</span> total disputes)
                    </p>
                  </div>
                  <div>
                    <nav className="relative z-0 inline-flex rounded-md shadow-sm -space-x-px">
                      <button
                        onClick={() => setCurrentPage(Math.max(0, currentPage - 1))}
                        disabled={currentPage === 0}
                        className="relative inline-flex items-center px-2 py-2 rounded-l-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
                      >
                        Previous
                      </button>
                      {[...Array(Math.min(5, disputesPage.totalPages))].map((_, idx) => {
                        const pageNum = idx;
                        return (
                          <button
                            key={pageNum}
                            onClick={() => setCurrentPage(pageNum)}
                            className={`relative inline-flex items-center px-4 py-2 border text-sm font-medium ${
                              currentPage === pageNum
                                ? 'z-10 bg-indigo-50 border-indigo-500 text-indigo-600'
                                : 'bg-white border-gray-300 text-gray-500 hover:bg-gray-50'
                            }`}
                          >
                            {pageNum + 1}
                          </button>
                        );
                      })}
                      <button
                        onClick={() => setCurrentPage(Math.min(disputesPage.totalPages - 1, currentPage + 1))}
                        disabled={currentPage === disputesPage.totalPages - 1}
                        className="relative inline-flex items-center px-2 py-2 rounded-r-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
                      >
                        Next
                      </button>
                    </nav>
                  </div>
                </div>
              </div>
            )}
            </div>
          ) : (
            <div className="text-center py-12 bg-white rounded-lg shadow">
              <p className="text-gray-500">
                {filterStatus === 'ALL'
                  ? 'No disputes found'
                  : `No disputes with status: ${filterStatus.replace('_', ' ')}`}
              </p>
            </div>
          )}
        </div>
      </main>

      {/* Status Update Modal */}
      {showStatusModal && selectedDispute && (
        <div className="fixed z-10 inset-0 overflow-y-auto">
          <div className="flex items-end justify-center min-h-screen pt-4 px-4 pb-20 text-center sm:block sm:p-0">
            <div className="fixed inset-0 bg-gray-500 bg-opacity-75 transition-opacity"></div>

            <div className="inline-block align-bottom bg-white rounded-lg text-left overflow-hidden shadow-xl transform transition-all sm:my-8 sm:align-middle sm:max-w-lg sm:w-full">
              <div className="bg-white px-4 pt-5 pb-4 sm:p-6 sm:pb-4">
                <div className="sm:flex sm:items-start">
                  <div className="mt-3 text-center sm:mt-0 sm:text-left w-full">
                    <h3 className="text-lg leading-6 font-medium text-gray-900 mb-4">
                      Update Dispute Status
                    </h3>

                    {error && (
                      <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-md">
                        <p className="text-sm text-red-600">{error}</p>
                      </div>
                    )}

                    <div className="mb-4 p-4 bg-gray-50 rounded-md">
                      <p className="text-sm text-gray-600">
                        <span className="font-medium">Dispute ID:</span> #{selectedDispute.id}
                      </p>
                      <p className="text-sm text-gray-600 mt-1">
                        <span className="font-medium">Customer:</span>{' '}
                        {selectedDispute.customerName}
                      </p>
                      <p className="text-sm text-gray-600 mt-1">
                        <span className="font-medium">Current Status:</span>{' '}
                        <span
                          className={`px-2 py-1 inline-flex text-xs leading-4 font-semibold rounded-full ${getStatusColor(
                            selectedDispute.status
                          )}`}
                        >
                          {selectedDispute.status.replace('_', ' ')}
                        </span>
                      </p>
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-2">
                        New Status
                      </label>
                      <select
                        value={newStatus}
                        onChange={(e) => setNewStatus(e.target.value as DisputeStatus)}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                      >
                        <option value={DisputeStatus.PENDING}>Pending</option>
                        <option value={DisputeStatus.UNDER_REVIEW}>Under Review</option>
                        <option value={DisputeStatus.MERCHANT_CONTACTED}>Merchant Contacted</option>
                        <option value={DisputeStatus.RESOLVED}>Resolved</option>
                        <option value={DisputeStatus.REJECTED}>Rejected</option>
                      </select>
                    </div>
                  </div>
                </div>
              </div>
              <div className="bg-gray-50 px-4 py-3 sm:px-6 sm:flex sm:flex-row-reverse">
                <button
                  type="button"
                  onClick={handleUpdateStatus}
                  disabled={updateStatusMutation.isPending || newStatus === selectedDispute.status}
                  className="w-full inline-flex justify-center rounded-md border border-transparent shadow-sm px-4 py-2 bg-indigo-600 text-base font-medium text-white hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 sm:ml-3 sm:w-auto sm:text-sm disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {updateStatusMutation.isPending ? 'Updating...' : 'Update Status'}
                </button>
                <button
                  type="button"
                  onClick={closeStatusModal}
                  disabled={updateStatusMutation.isPending}
                  className="mt-3 w-full inline-flex justify-center rounded-md border border-gray-300 shadow-sm px-4 py-2 bg-white text-base font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 sm:mt-0 sm:ml-3 sm:w-auto sm:text-sm"
                >
                  Cancel
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
