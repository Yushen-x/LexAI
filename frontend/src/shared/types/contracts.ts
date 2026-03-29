import type { ApiResponse } from '@/shared/types/legal';

export type ContractStatus =
  | 'DRAFT'
  | 'UNDER_REVIEW'
  | 'SIGNED'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'TERMINATED';

export interface ContractItem {
  id: number;
  contractNo: string;
  name: string;
  contractType: string;
  partyA: string;
  partyB: string;
  amount: number;
  status: ContractStatus;
  source: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ContractListResult {
  content: ContractItem[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

export type ApiContractListResponse = ApiResponse<ContractListResult>;
export type ApiContractItemResponse = ApiResponse<ContractItem>;
