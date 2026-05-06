import axios, { AxiosError, AxiosInstance, InternalAxiosRequestConfig } from 'axios';
import {
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  Transaction,
  Dispute,
  CreateDisputeRequest,
  UpdateDisputeStatusRequest,
  ApiError,
  Page,
} from '../types';

const API_BASE_URL = 'http://localhost:8080';

class ApiService {
  private api: AxiosInstance;

  constructor() {
    this.api = axios.create({
      baseURL: API_BASE_URL,
      headers: {
        'Content-Type': 'application/json',
      },
    });

    // Request interceptor to add JWT token
    this.api.interceptors.request.use(
      (config: InternalAxiosRequestConfig) => {
        const token = localStorage.getItem('token');
        if (token && config.headers) {
          config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
      },
      (error) => {
        return Promise.reject(error);
      }
    );

    // Response interceptor for error handling
    this.api.interceptors.response.use(
      (response) => response,
      (error: AxiosError<ApiError>) => {
        if (error.response?.status === 401) {
          // Only redirect to login if this is NOT a login/register request
          const isAuthRequest = error.config?.url?.includes('/api/auth/login') ||
                                 error.config?.url?.includes('/api/auth/register');

          if (!isAuthRequest) {
            // Clear token and redirect to login for authenticated requests that fail
            localStorage.removeItem('token');
            localStorage.removeItem('user');
            window.location.href = '/login';
          }
        }
        return Promise.reject(this.handleError(error));
      }
    );
  }

  private handleError(error: AxiosError<ApiError>): ApiError {
    if (error.response?.data) {
      return {
        message: error.response.data.message || 'An error occurred',
        status: error.response.status,
        errors: error.response.data.errors,
      };
    }
    return {
      message: error.message || 'Network error occurred',
      status: error.response?.status,
    };
  }

  // Auth endpoints
  async register(data: RegisterRequest): Promise<AuthResponse> {
    // Backend expects 'username' field but we use 'email' in frontend
    const payload = {
      username: data.email,
      password: data.password,
      fullName: data.fullName,
      email: data.email
    };
    const response = await this.api.post<AuthResponse>('/api/auth/register', payload);
    return response.data;
  }

  async login(data: LoginRequest): Promise<AuthResponse> {
    // Backend expects 'username' field but we use 'email' in frontend
    const payload = {
      username: data.email,
      password: data.password
    };
    const response = await this.api.post<AuthResponse>('/api/auth/login', payload);
    return response.data;
  }

  // Transaction endpoints
  async getTransactions(page: number = 0, size: number = 20): Promise<Page<Transaction>> {
    const response = await this.api.get<Page<Transaction>>('/api/transactions', {
      params: { page, size }
    });
    return response.data;
  }

  async getDisputeableTransactions(page: number = 0, size: number = 20): Promise<Page<Transaction>> {
    const response = await this.api.get<Page<Transaction>>('/api/transactions/disputeable', {
      params: { page, size }
    });
    return response.data;
  }

  // Dispute endpoints
  async createDispute(data: CreateDisputeRequest): Promise<Dispute> {
    const response = await this.api.post<Dispute>('/api/disputes', data);
    return response.data;
  }

  async getMyDisputes(): Promise<Dispute[]> {
    // Customer disputes - NOT paginated (customers have few disputes)
    const response = await this.api.get<Dispute[]>('/api/disputes/my-disputes');
    return response.data;
  }

  async getAllDisputes(page: number = 0, size: number = 20, sortBy: string = 'createdAt', direction: string = 'DESC'): Promise<Page<Dispute>> {
    // Admin disputes - PAGINATED (admins view all disputes)
    const response = await this.api.get<Page<Dispute>>('/api/disputes', {
      params: { page, size, sortBy, direction }
    });
    return response.data;
  }

  async updateDisputeStatus(
    disputeId: number,
    data: UpdateDisputeStatusRequest
  ): Promise<Dispute> {
    const response = await this.api.put<Dispute>(`/api/disputes/${disputeId}/status`, data);
    return response.data;
  }

  async getDisputeStats(): Promise<Record<string, number>> {
    const response = await this.api.get<Record<string, number>>('/api/disputes/stats');
    return response.data;
  }
}

export const apiService = new ApiService();
