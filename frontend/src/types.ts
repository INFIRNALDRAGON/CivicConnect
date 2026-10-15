export type Role = 'REQUESTER' | 'STAFF' | 'MANAGER';
export type Status = 'SUBMITTED' | 'ASSIGNED' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED' | 'REJECTED';
export type Priority = 'LOW' | 'NORMAL' | 'HIGH';

export interface User {
  id: number;
  name: string;
  email: string;
  role: Role;
}

export interface Session {
  token: string;
  user: User;
}

export interface Category {
  id: number;
  name: string;
}

export interface HistoryEntry {
  id: number;
  previousStatus: Status | null;
  newStatus: Status;
  changedAt: string;
  comment: string | null;
  changedByName?: string;
}

export interface ServiceRequest {
  id: number;
  referenceNumber: string;
  categoryId: number;
  categoryName: string;
  description: string;
  status: Status;
  priority: Priority;
  createdAt: string;
  updatedAt: string;
  resolvedAt: string | null;
  assigned: boolean;
  overdue: boolean;
  version: number;
  assignedStaffName?: string | null;
  assignedAt?: string | null;
  requesterReference?: string;
  allowedTransitions?: Status[];
  history?: HistoryEntry[];
}

export interface NewRequest {
  categoryId: number;
  priority: Priority;
  description: string;
}

export interface QueueFilters {
  search: string;
  categoryId: string;
  status: string;
  sort: string;
}

export interface CategorySummary {
  categoryId: number;
  categoryName: string;
  total: number;
  open: number;
  overdue: number;
  resolved: number;
  closed: number;
  avgResolutionHours: number | null;
}

export interface ReportRequest {
  id: number;
  referenceNumber: string;
  categoryName: string;
  status: Status;
  createdAt: string;
  overdue: boolean;
}

export interface Report {
  total: number;
  totals: Record<Status, number>;
  openCount: number;
  overdueCount: number;
  byCategory: CategorySummary[];
  requests: ReportRequest[];
}
