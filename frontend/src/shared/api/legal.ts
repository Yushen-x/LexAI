import { api } from './http';
import type {
  CaseAnalysisRequest,
  CaseAnalysisResponse,
  ConsultationRequest,
  ConsultationResponse,
  ContractReviewRequest,
  ContractReviewResponse,
  LegalScenarioType,
  LegalSessionDetail,
  LegalSessionListResult,
  LegalSessionSummary,
  PlatformOverview,
  SystemHealth
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

export async function fetchHealth(): Promise<SystemHealth> {
  return api.get<SystemHealth>('/system/health');
}

export async function fetchLegalSessions(
  type: LegalScenarioType,
  page = 0,
  size = 20,
  keyword?: string
): Promise<LegalSessionListResult> {
  return api.get<LegalSessionListResult>('/legal/sessions', {
    params: {
      type,
      page,
      size,
      keyword: keyword?.trim() || undefined
    }
  });
}

export async function fetchRecentLegalSessions(limit = 5): Promise<LegalSessionSummary[]> {
  return api.get<LegalSessionSummary[]>('/legal/sessions/recent', {
    params: { limit }
  });
}

export async function fetchLegalSessionDetail(id: number): Promise<LegalSessionDetail> {
  return api.get<LegalSessionDetail>(`/legal/sessions/${id}`);
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

