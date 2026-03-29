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

export interface ConsultationRequest {
  question: string;
  facts: string[];
}

export interface ConsultationResponse {
  category: string;
  legalBasis: string[];
  recommendations: string[];
  riskAlerts: string[];
}

export interface CaseAnalysisRequest {
  caseSummary: string;
  evidencePoints: string[];
}

export interface CaseAnalysisResponse {
  keyFacts: string[];
  disputedIssues: string[];
  evidenceGaps: string[];
  suggestedActions: string[];
}

export interface ContractReviewRequest {
  contractTitle: string;
  contractContent: string;
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
}

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

