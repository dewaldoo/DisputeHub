import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '../contexts/AuthContext';
import { apiService } from '../services/api';
import { Transaction, CreateDisputeRequest, ApiError } from '../types';

export const CustomerDashboard: React.FC = () => {
  const { user, logout } = useAuth();
  const queryClient = useQueryClient();
  const [selectedTab, setSelectedTab] = useState<'transactions' | 'disputes'>('transactions');
  const [showDisputeModal, setShowDisputeModal] = useState(false);
  const [selectedTransaction, setSelectedTransaction] = useState<Transaction | null>(null);
  const [disputeReason, setDisputeReason] = useState('');
  const [disputeDescription, setDisputeDescription] = useState('');
  const [error, setError] = useState('');
  const [transactionPage, setTransactionPage] = useState(0);

  // Fetch transactions (paginated)
  const { data: transactionsPage, isLoading: transactionsLoading } = useQuery({
    queryKey: ['transactions', transactionPage, 2],
    queryFn: () => apiService.getTransactions(transactionPage, 2),
  });

  // Fetch disputeable transactions (paginated - first page only)
  const { data: disputeableTransactionsPage } = useQuery({
    queryKey: ['disputeableTransactions', 20],
    queryFn: () => apiService.getDisputeableTransactions(0, 20),
  });

  // Extract content arrays from pages
  const transactions = transactionsPage?.content || [];
  const disputeableTransactions = disputeableTransactionsPage?.content || [];

  // Fetch user's disputes
  const { data: disputes, isLoading: disputesLoading } = useQuery({
    queryKey: ['myDisputes'],
    queryFn: () => apiService.getMyDisputes(),
  });

  // Create dispute mutation
  const createDisputeMutation = useMutation({
    mutationFn: (data: CreateDisputeRequest) => apiService.createDispute(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['myDisputes'] });
      queryClient.invalidateQueries({ queryKey: ['disputeableTransactions'] });
      queryClient.invalidateQueries({ queryKey: ['transactions'] });
      setShowDisputeModal(false);
      setSelectedTransaction(null);
      setDisputeReason('');
      setDisputeDescription('');
      setError('');
    },
    onError: (err: ApiError) => {
      setError(err.message || 'Failed to create dispute');
    },
  });

  const handleCreateDispute = () => {
    if (!selectedTransaction) return;
    setError('');

    if (!disputeReason.trim() || !disputeDescription.trim()) {
      setError('Please provide both reason and description');
      return;
    }

    createDisputeMutation.mutate({
      transactionId: selectedTransaction.id,
      reason: disputeReason,
      description: disputeDescription,
    });
  };

  const openDisputeModal = (transaction: Transaction) => {
    setSelectedTransaction(transaction);
    setShowDisputeModal(true);
    setError('');
  };

  const closeDisputeModal = () => {
    setShowDisputeModal(false);
    setSelectedTransaction(null);
    setDisputeReason('');
    setDisputeDescription('');
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
    });
  };

  const getStatusColor = (status: string) => {
    if (!status) return 'bg-gray-100 text-gray-800';
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

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white shadow">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
          <div className="flex justify-between items-center">
            <div>
              <h1 className="text-2xl font-bold text-gray-900">DisputeHub</h1>
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
        {/* Tabs */}
        <div className="mb-6 border-b border-gray-200">
          <nav className="-mb-px flex space-x-8">
            <button
              onClick={() => setSelectedTab('transactions')}
              className={`${
                selectedTab === 'transactions'
                  ? 'border-indigo-500 text-indigo-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              } whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm`}
            >
              Transactions
            </button>
            <button
              onClick={() => setSelectedTab('disputes')}
              className={`${
                selectedTab === 'disputes'
                  ? 'border-indigo-500 text-indigo-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              } whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm`}
            >
              My Disputes
            </button>
          </nav>
        </div>

        {/* Transactions Tab */}
        {selectedTab === 'transactions' && (
          <div>
            <div className="mb-4 flex justify-between items-center">
              <h2 className="text-xl font-semibold text-gray-900">Your Transactions</h2>
            </div>

            {transactionsLoading ? (
              <div className="text-center py-12">
                <div className="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600"></div>
                <p className="mt-2 text-gray-600">Loading transactions...</p>
              </div>
            ) : transactions && transactions.length > 0 ? (
              <div className="bg-white shadow overflow-hidden rounded-lg">
                <table className="min-w-full divide-y divide-gray-200">
                  <thead className="bg-gray-50">
                    <tr>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                        Transaction ID
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                        Merchant
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                        Amount
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                        Date
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                        Category
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                        Status
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                        Actions
                      </th>
                    </tr>
                  </thead>
                  <tbody className="bg-white divide-y divide-gray-200">
                    {transactions.map((transaction) => (
                      <tr key={transaction.id}>
                        <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                          {transaction.referenceNumber}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                          {transaction.merchantName}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                          {formatCurrency(transaction.amount)}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                          {formatDate(transaction.transactionDate)}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                          {transaction.category}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <span
                            className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${
                              transaction.hasDispute ? 'bg-red-100 text-red-800' : 'bg-green-100 text-green-800'
                            }`}
                          >
                            {transaction.hasDispute ? 'Disputed' : 'Active'}
                          </span>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                          {disputeableTransactions?.some((dt) => dt.id === transaction.id) ? (
                            <button
                              onClick={() => openDisputeModal(transaction)}
                              className="text-indigo-600 hover:text-indigo-900 font-medium"
                            >
                              Dispute
                            </button>
                          ) : (
                            <span className="text-gray-400">Not Disputable</span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>

                {/* Pagination for Transactions */}
                {transactionsPage && transactionsPage.totalPages > 1 && (
                  <div className="bg-white px-4 py-3 flex items-center justify-between border-t border-gray-200 sm:px-6">
                    <div className="flex-1 flex justify-between sm:hidden">
                      <button
                        onClick={() => setTransactionPage(Math.max(0, transactionPage - 1))}
                        disabled={transactionPage === 0}
                        className="relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50"
                      >
                        Previous
                      </button>
                      <button
                        onClick={() => setTransactionPage(Math.min(transactionsPage.totalPages - 1, transactionPage + 1))}
                        disabled={transactionPage === transactionsPage.totalPages - 1}
                        className="ml-3 relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50"
                      >
                        Next
                      </button>
                    </div>
                    <div className="hidden sm:flex-1 sm:flex sm:items-center sm:justify-between">
                      <div>
                        <p className="text-sm text-gray-700">
                          Page <span className="font-medium">{transactionPage + 1}</span> of{' '}
                          <span className="font-medium">{transactionsPage.totalPages}</span>
                        </p>
                      </div>
                      <div>
                        <button
                          onClick={() => setTransactionPage(Math.max(0, transactionPage - 1))}
                          disabled={transactionPage === 0}
                          className="relative inline-flex items-center px-3 py-2 rounded-l-md border border-gray-300 bg-white text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50"
                        >
                          Previous
                        </button>
                        <button
                          onClick={() => setTransactionPage(Math.min(transactionsPage.totalPages - 1, transactionPage + 1))}
                          disabled={transactionPage === transactionsPage.totalPages - 1}
                          className="ml-3 relative inline-flex items-center px-3 py-2 rounded-r-md border border-gray-300 bg-white text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50"
                        >
                          Next
                        </button>
                      </div>
                    </div>
                  </div>
                )}
              </div>
            ) : (
              <div className="text-center py-12 bg-white rounded-lg shadow">
                <p className="text-gray-500">No transactions found</p>
              </div>
            )}
          </div>
        )}

        {/* Disputes Tab */}
        {selectedTab === 'disputes' && (
          <div>
            <div className="mb-4">
              <h2 className="text-xl font-semibold text-gray-900">Your Disputes</h2>
            </div>

            {disputesLoading ? (
              <div className="text-center py-12">
                <div className="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600"></div>
                <p className="mt-2 text-gray-600">Loading disputes...</p>
              </div>
            ) : disputes && disputes.length > 0 ? (
              <div className="space-y-4">
                {disputes.map((dispute) => (
                  <div key={dispute.id} className="bg-white shadow rounded-lg p-6">
                    <div className="flex justify-between items-start mb-4">
                      <div>
                        <h3 className="text-lg font-medium text-gray-900">
                          Dispute #{dispute.id}
                        </h3>
                        <p className="text-sm text-gray-500 mt-1">
                          Transaction ID: {dispute.transactionId}
                        </p>
                      </div>
                      <span
                        className={`px-3 py-1 inline-flex text-xs leading-5 font-semibold rounded-full ${getStatusColor(
                          dispute.status
                        )}`}
                      >
                        {dispute.status.replace('_', ' ')}
                      </span>
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
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

                    <div className="border-t border-gray-200 pt-4">
                      <p className="text-sm font-medium text-gray-500">Reason</p>
                      <p className="text-sm text-gray-900 mt-1">{dispute.reason}</p>
                    </div>

                    <div className="mt-4">
                      <p className="text-sm font-medium text-gray-500">Description</p>
                      <p className="text-sm text-gray-900 mt-1">{dispute.description}</p>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="text-center py-12 bg-white rounded-lg shadow">
                <p className="text-gray-500">No disputes found</p>
              </div>
            )}
          </div>
        )}
      </main>

      {/* Dispute Modal */}
      {showDisputeModal && selectedTransaction && (
        <div className="fixed z-10 inset-0 overflow-y-auto">
          <div className="flex items-end justify-center min-h-screen pt-4 px-4 pb-20 text-center sm:block sm:p-0">
            <div className="fixed inset-0 bg-gray-500 bg-opacity-75 transition-opacity"></div>

            <div className="inline-block align-bottom bg-white rounded-lg text-left overflow-hidden shadow-xl transform transition-all sm:my-8 sm:align-middle sm:max-w-lg sm:w-full">
              <div className="bg-white px-4 pt-5 pb-4 sm:p-6 sm:pb-4">
                <div className="sm:flex sm:items-start">
                  <div className="mt-3 text-center sm:mt-0 sm:text-left w-full">
                    <h3 className="text-lg leading-6 font-medium text-gray-900 mb-4">
                      Create Dispute
                    </h3>

                    {error && (
                      <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-md">
                        <p className="text-sm text-red-600">{error}</p>
                      </div>
                    )}

                    <div className="mb-4 p-4 bg-gray-50 rounded-md">
                      <p className="text-sm text-gray-600">
                        <span className="font-medium">Transaction:</span>{' '}
                        {selectedTransaction.referenceNumber}
                      </p>
                      <p className="text-sm text-gray-600 mt-1">
                        <span className="font-medium">Merchant:</span>{' '}
                        {selectedTransaction.merchantName}
                      </p>
                      <p className="text-sm text-gray-600 mt-1">
                        <span className="font-medium">Amount:</span>{' '}
                        {formatCurrency(selectedTransaction.amount)}
                      </p>
                    </div>

                    <div className="space-y-4">
                      <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">
                          Reason
                        </label>
                        <input
                          type="text"
                          value={disputeReason}
                          onChange={(e) => setDisputeReason(e.target.value)}
                          className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                          placeholder="e.g., Unauthorized transaction"
                        />
                      </div>

                      <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">
                          Description
                        </label>
                        <textarea
                          value={disputeDescription}
                          onChange={(e) => setDisputeDescription(e.target.value)}
                          rows={4}
                          className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                          placeholder="Provide detailed information about the dispute..."
                        />
                      </div>
                    </div>
                  </div>
                </div>
              </div>
              <div className="bg-gray-50 px-4 py-3 sm:px-6 sm:flex sm:flex-row-reverse">
                <button
                  type="button"
                  onClick={handleCreateDispute}
                  disabled={createDisputeMutation.isPending}
                  className="w-full inline-flex justify-center rounded-md border border-transparent shadow-sm px-4 py-2 bg-indigo-600 text-base font-medium text-white hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 sm:ml-3 sm:w-auto sm:text-sm disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {createDisputeMutation.isPending ? 'Creating...' : 'Create Dispute'}
                </button>
                <button
                  type="button"
                  onClick={closeDisputeModal}
                  disabled={createDisputeMutation.isPending}
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
