import { api } from './http';
import type {
  TaskItem,
  WorkspaceTaskStatus,
} from '@/shared/types/tasks';

export async function fetchTasks(status?: WorkspaceTaskStatus): Promise<TaskItem[]> {
  return api.get<TaskItem[]>('/tasks', {
    params: status ? { status } : {},
  });
}

export async function getTask(id: number): Promise<TaskItem> {
  return api.get<TaskItem>(`/tasks/${id}`);
}

export async function updateTaskStatus(id: number, status: WorkspaceTaskStatus): Promise<TaskItem> {
  return api.put<TaskItem>(`/tasks/${id}/status`, { status });
}
