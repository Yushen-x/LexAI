import { api } from './http';
import type {
  ContractItem,
  ContractListResult,
  ContractReviewDecision,
  ContractStatus
} from '@/shared/types/contracts';

export interface FetchContractsParams {
  keyword?: string;
  status?: ContractStatus;
  type?: string;
  page?: number;
  size?: number;
}

export interface CreateContractRequest {
  name: string;
  contractType: string;
  partyA?: string;
  partyB?: string;
  amount?: number;
  content?: string;
  source?: string;
  status?: ContractStatus;
}

export interface UpdateContractRequest extends CreateContractRequest {}

export interface UpdateContractReviewRequest {
  reviewerOpinion?: string;
  reviewDecision?: ContractReviewDecision;
}

export async function fetchContracts(params: FetchContractsParams): Promise<ContractListResult> {
  return api.get<ContractListResult>('/contracts', {
    params: {
      keyword: params.keyword || undefined,
      status: params.status || undefined,
      type: params.type || undefined,
      page: params.page ?? 0,
      size: params.size ?? 20
    }
  });
}

export async function getContract(id: number): Promise<ContractItem> {
  return api.get<ContractItem>(`/contracts/${id}`);
}

export async function updateContractStatus(id: number, status: ContractStatus): Promise<ContractItem> {
  return api.put<ContractItem>(`/contracts/${id}/status`, { status });
}

export async function createContract(payload: CreateContractRequest): Promise<ContractItem> {
  return api.post<ContractItem>('/contracts', payload);
}

export async function updateContract(id: number, payload: UpdateContractRequest): Promise<ContractItem> {
  return api.put<ContractItem>(`/contracts/${id}`, payload);
}

export async function updateContractReview(id: number, payload: UpdateContractReviewRequest): Promise<ContractItem> {
  return api.put<ContractItem>(`/contracts/${id}/review`, payload);
}

export interface ContractStatsSummary {
  total: number;
  statusDistribution: { status: string; count: number }[];
  typeDistribution: { type: string; count: number }[];
  monthlyTrend: { year: number; month: number; count: number }[];
}

export async function fetchContractStatistics(): Promise<ContractStatsSummary> {
  return api.get<ContractStatsSummary>('/contracts/statistics');
}
