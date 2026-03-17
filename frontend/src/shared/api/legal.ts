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

export async function fetchOverview() {
  const { data } = await http.get<ApiResponse<PlatformOverview>>('/system/overview');
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

