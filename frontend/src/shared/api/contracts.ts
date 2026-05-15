import { http } from './http';
import type {
  ApiContractItemResponse,
  ApiContractListResponse,
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
  const { data } = await http.get<ApiContractListResponse>('/contracts', {
    params: {
      keyword: params.keyword || undefined,
      status: params.status || undefined,
      type: params.type || undefined,
      page: params.page ?? 0,
      size: params.size ?? 20
    }
  });
  return data.data;
}

export async function getContract(id: number): Promise<ContractItem> {
  const { data } = await http.get<ApiContractItemResponse>(`/contracts/${id}`);
  return data.data;
}

export async function updateContractStatus(id: number, status: ContractStatus): Promise<ContractItem> {
  const { data } = await http.put<ApiContractItemResponse>(`/contracts/${id}/status`, { status });
  return data.data;
}

export async function createContract(payload: CreateContractRequest): Promise<ContractItem> {
  const { data } = await http.post<ApiContractItemResponse>('/contracts', payload);
  return data.data;
}

export async function updateContract(id: number, payload: UpdateContractRequest): Promise<ContractItem> {
  const { data } = await http.put<ApiContractItemResponse>(`/contracts/${id}`, payload);
  return data.data;
}

export async function updateContractReview(id: number, payload: UpdateContractReviewRequest): Promise<ContractItem> {
  const { data } = await http.put<ApiContractItemResponse>(`/contracts/${id}/review`, payload);
  return data.data;
}
