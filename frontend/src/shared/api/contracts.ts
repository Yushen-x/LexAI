import { http } from './http';
import type { ApiContractItemResponse, ApiContractListResponse, ContractItem, ContractListResult, ContractStatus } from '@/shared/types/contracts';

export interface FetchContractsParams {
  keyword?: string;
  status?: ContractStatus;
  type?: string;
  page?: number;
  size?: number;
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
