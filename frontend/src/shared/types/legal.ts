export interface ApiResponse<T> {
  code: string;
  message: string;
  data: T;
  timestamp: string;
}

export interface PlatformOverview {
  projectName: string;
  positioning: string;
  technicalHighlights: string[];
  capabilities: string[];
}

export interface RetrievalContext {
  laws: string[];
  cases: string[];
  knowledge: string[];
}

export interface ConsultationRequest {
  question: string;
  facts: string[];
  createFollowUpTask?: boolean;
}

export interface ConsultationResponse {
  category: string;
  legalBasis: string[];
  recommendations: string[];
  riskAlerts: string[];
  confidence?: number | null;
  retrievalContext: RetrievalContext;
  answer?: string | null;
}

export interface CaseAnalysisRequest {
  caseSummary: string;
  evidencePoints: string[];
  createFollowUpTask?: boolean;
}

export interface CaseAnalysisResponse {
  keyFacts: string[];
  disputedIssues: string[];
  evidenceGaps: string[];
  suggestedActions: string[];
  confidence?: number | null;
  retrievalContext: RetrievalContext;
}

export interface ContractReviewRequest {
  contractTitle: string;
  contractContent: string;
  contractId?: number;
  createFollowUpTask?: boolean;
}

export interface ContractRiskItem {
  level: 'LOW' | 'MEDIUM' | 'HIGH';
  clause: string;
  issue: string;
  suggestion: string;
}

export interface ContractReviewResponse {
  risks: ContractRiskItem[];
  missingClauses: string[];
  summary: string;
  confidence?: number | null;
  retrievalContext: RetrievalContext;
}

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
  confidence?: number | null;
  retrievalContext?: RetrievalContext;
}

