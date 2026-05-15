import type { ApiResponse, ContractRiskItem } from '@/shared/types/legal';

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
  content: string;
  status: ContractStatus;
  source: string | null;
  latestReview: ContractLatestReview | null;
  createdAt: string;
  updatedAt: string;
}

export type ContractReviewDecision = 'PENDING_CONFIRMATION' | 'NEEDS_REVISION' | 'APPROVED';

export interface ContractLatestReview {
  summary: string;
  risks: ContractRiskItem[];
  missingClauses: string[];
  reviewerOpinion: string;
  reviewDecision: ContractReviewDecision;
  reviewedAt: string | null;
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
