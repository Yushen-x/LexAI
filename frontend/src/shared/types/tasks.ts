import type { ApiResponse } from '@/shared/types/legal';

export type WorkspaceTaskStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'REJECTED';

export type WorkspaceTaskType =
  | 'LEGAL_CONSULTATION'
  | 'CASE_ANALYSIS'
  | 'CONTRACT_REVIEW'
  | 'CONTRACT_DRAFT';

export interface TaskItem {
  id: number;
  taskNo: string;
  title: string;
  type: WorkspaceTaskType;
  relatedId: string;
  initiator: string;
  status: WorkspaceTaskStatus;
  createdAt: string;
}

export type ApiTaskListResponse = ApiResponse<TaskItem[]>;
export type ApiTaskItemResponse = ApiResponse<TaskItem>;
