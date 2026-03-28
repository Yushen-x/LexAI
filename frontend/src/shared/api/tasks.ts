import { http } from './http';
import type {
  ApiTaskItemResponse,
  ApiTaskListResponse,
  TaskItem,
  WorkspaceTaskStatus,
} from '@/shared/types/tasks';

export async function fetchTasks(status?: WorkspaceTaskStatus): Promise<TaskItem[]> {
  const { data } = await http.get<ApiTaskListResponse>('/tasks', {
    params: status ? { status } : {},
  });
  return data.data;
}

export async function getTask(id: number): Promise<TaskItem> {
  const { data } = await http.get<ApiTaskItemResponse>(`/tasks/${id}`);
  return data.data;
}

export async function updateTaskStatus(id: number, status: WorkspaceTaskStatus): Promise<TaskItem> {
  const { data } = await http.put<ApiTaskItemResponse>(`/tasks/${id}/status`, { status });
  return data.data;
}
