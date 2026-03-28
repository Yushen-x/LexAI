import { http } from './http';
import type {
  ApiResponse,
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
}

export interface ContractDraftResponse {
  generatedContent: string;
  summary: string;
  generatedAt: string;
}

export async function fetchOverview() {
  const { data } = await http.get<ApiResponse<PlatformOverview>>('/system/overview');
  return data.data;
}

export async function fetchHealth(): Promise<Record<string, string>> {
  const { data } = await http.get<ApiResponse<Record<string, string>>>('/system/health');
  return data.data;
}

export async function submitConsultation(payload: ConsultationRequest) {
  const { data } = await http.post<ApiResponse<ConsultationResponse>>('/legal/consultation', payload);
  return data.data;
}

export async function submitCaseAnalysis(payload: CaseAnalysisRequest) {
  const { data } = await http.post<ApiResponse<CaseAnalysisResponse>>('/legal/case-analysis', payload);
  return data.data;
}

export async function submitContractReview(payload: ContractReviewRequest) {
  const { data } = await http.post<ApiResponse<ContractReviewResponse>>('/legal/contract-review', payload);
  return data.data;
}

export async function submitContractDraft(payload: ContractDraftRequest) {
  const { data } = await http.post<ApiResponse<ContractDraftResponse>>('/legal/contract-draft', payload);
  return data.data;
}

