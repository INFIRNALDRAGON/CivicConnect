export type Role = 'REQUESTER' | 'STAFF' | 'MANAGER';
export type Status = 'SUBMITTED' | 'ASSIGNED' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED' | 'REJECTED';
export type Priority = 'LOW' | 'MEDIUM' | 'HIGH';

export interface User {
  id: string;
  name: string;
  email: string;
  role: Role;
}

export interface Session {
  token: string;
  user: User;
}

export interface Category {
  id: string;
  name: string;
}

export interface HistoryEntry {
  id: string;
  previousStatus: Status | null;
  newStatus: Status;
  changedAt: string;
  comment: string | null;
  changedByName?: string;
}

export interface ServiceRequest {
  id: string;
  referenceNumber: string;
  categoryId: string;
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
  categoryId: string;
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
  categoryId: string;
  categoryName: string;
  total: number;
  open: number;
  overdue: number;
  resolved: number;
  closed: number;
  avgResolutionHours: number | null;
}

export interface ReportRequest {
  id: string;
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

export interface AppNotification {
  id: string;
  requestId: string;
  referenceNumber: string;
  message: string;
  createdAt: string;
  readAt: string | null;
}
