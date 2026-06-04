import { api } from './http';
import type {
  CaseAnalysisRequest,
  CaseAnalysisResponse,
  ConsultationRequest,
  ConsultationResponse,
  ContractReviewRequest,
  ContractReviewResponse,
  PlatformOverview
} from '@/shared/types/legal';

export interface ContractDraftRequest {
  contractName: string;
  contractType: string;
  partyA: string;
  partyB: string;
  amount: number;
  duration?: string;
  requirements?: string;
  createFollowUpTask?: boolean;
}

export interface ContractDraftResponse {
  title: string | null;
  generatedContent: string;
  summary: string;
  generatedAt: string;
}

export async function fetchOverview() {
  return api.get<PlatformOverview>('/system/overview');
}

export async function fetchHealth(): Promise<Record<string, string>> {
  return api.get<Record<string, string>>('/system/health');
}

export async function submitConsultation(payload: ConsultationRequest) {
  return api.post<ConsultationResponse>('/legal/consultation', payload);
}

export async function submitCaseAnalysis(payload: CaseAnalysisRequest) {
  return api.post<CaseAnalysisResponse>('/legal/case-analysis', payload);
}

export async function submitContractReview(payload: ContractReviewRequest) {
  return api.post<ContractReviewResponse>('/legal/contract-review', payload);
}

export async function submitContractDraft(payload: ContractDraftRequest) {
  return api.post<ContractDraftResponse>('/legal/contract-draft', payload);
}

