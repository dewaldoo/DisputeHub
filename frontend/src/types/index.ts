// User and Authentication Types
export interface User {
  id: number;
  email: string;
  fullName: string;
  role: 'CUSTOMER' | 'ADMIN';
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  fullName: string;
}

export interface AuthResponse {
  token: string;
  type: string;
  id: number;
  username: string;
  role: string;
}

// Transaction Types
export interface Transaction {
  id: number;
  merchantName: string;
  amount: number;
  transactionDate: string;
  category: string;
  description?: string;
  referenceNumber?: string;
  createdAt: string;
  // Flattened user details
  userId: number;
  username: string;
  // Flattened dispute status
  hasDispute: boolean;
  disputeId?: number;
  // Legacy support
  transactionId?: string;
  status?: string;
  customerId?: number;
}

// Dispute Types
export enum DisputeStatus {
  PENDING = 'PENDING',
  UNDER_REVIEW = 'UNDER_REVIEW',
  MERCHANT_CONTACTED = 'MERCHANT_CONTACTED',
  RESOLVED = 'RESOLVED',
  REJECTED = 'REJECTED'
}

export interface Dispute {
  id: number;
  reason: string;
  description: string;
  status: string; // Backend returns enum as string
  evidenceUrl?: string;
  resolutionNotes?: string;
  createdAt: string;
  updatedAt: string;
  resolvedAt?: string;
  // Flattened transaction details
  transactionId: number;
  merchantName: string;
  amount: number;
  transactionDate: string;
  transactionCategory?: string;
  // Flattened user details
  userId: number;
  username: string;
  customerName?: string;
  customerEmail?: string;
  // Legacy support for nested transaction
  transaction?: Transaction;
  customerId?: number;
}

export interface CreateDisputeRequest {
  transactionId: number;
  reason: string;
  description: string;
}

export interface UpdateDisputeStatusRequest {
  status: DisputeStatus;
}

// Pagination Types (Spring Data Page)
export interface Page<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
    sort: {
      sorted: boolean;
      unsorted: boolean;
      empty: boolean;
    };
    offset: number;
    paged: boolean;
    unpaged: boolean;
  };
  totalPages: number;
  totalElements: number;
  last: boolean;
  first: boolean;
  size: number;
  number: number;
  sort: {
    sorted: boolean;
    unsorted: boolean;
    empty: boolean;
  };
  numberOfElements: number;
  empty: boolean;
}

// API Error Response
export interface ApiError {
  message: string;
  status?: number;
  errors?: Record<string, string[]>;
}
