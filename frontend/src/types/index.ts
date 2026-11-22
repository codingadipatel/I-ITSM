export type Role = "EMPLOYEE" | "TECHNICIAN" | "ADMIN";
export type Status =
  | "NEW"
  | "CLASSIFIED"
  | "ASSIGNED"
  | "ACKNOWLEDGED"
  | "IN_PROGRESS"
  | "RESOLVED"
  | "VERIFIED"
  | "CLOSED";
export type Priority = "P1" | "P2" | "P3" | "P4";
export interface User {
  id: number;
  name: string;
  email: string;
  role: Role;
  active: boolean;
  available: boolean;
  maxWorkload: number;
  experienceYears: number;
  skills: string[];
}
export interface Catalog {
  id: number;
  name: string;
  active: boolean;
  criticality: number;
  requiredSkill: string;
}
export interface Incident {
  id: number;
  version: number;
  title: string;
  description: string;
  reporter: User;
  technician: User | null;
  service: Catalog;
  category: Catalog;
  impact: number;
  urgency: number;
  affectedUsers: number;
  priority: Priority;
  priorityScore: number;
  priorityReason: string;
  assignmentReason: string;
  status: Status;
  createdAt: string;
  updatedAt: string;
  responseDeadline: string;
  resolutionDeadline: string;
  acknowledgedAt: string | null;
  resolvedAt: string | null;
  verifiedAt: string | null;
  closedAt: string | null;
  resolutionDetails: string | null;
  verificationDetails: string | null;
  slaLevel: string;
  responseViolated: boolean;
  resolutionViolated: boolean;
}
export interface IncidentInput {
  title: string;
  description: string;
  serviceId: number;
  categoryId: number;
  impact: number;
  urgency: number;
  affectedUsers: number;
}
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
}
export interface Audit {
  id: number;
  incidentId: number | null;
  actor: string;
  action: string;
  previousState: string;
  newState: string;
  description: string;
  createdAt: string;
}
export interface Note {
  id: number;
  author: User;
  content: string;
  informationRequest: boolean;
  createdAt: string;
}
export interface Duplicate {
  id: number;
  incidentId: number;
  relatedId: number;
  relatedTitle: string;
  score: number;
  decision: string;
}
export interface Detail {
  incident: Incident;
  notes: Note[];
  history: Audit[];
  duplicates: Duplicate[];
  feedback: { rating?: number; comment?: string };
}
export interface Notification {
  id: number;
  incidentId: number | null;
  eventType: string;
  message: string;
  read: boolean;
  createdAt: string;
  conversation: {
    technicianId: number;
    participantName: string;
    canReply: boolean;
  } | null;
}
export interface Metric {
  name: string;
  value: number;
  id?: number;
  capacity?: number;
  available?: boolean;
}
export interface Overview {
  total: number;
  open: number;
  critical: number;
  resolved: number;
  closed: number;
  slaViolations: number;
  slaCompliance: number;
  avgResponseMinutes: number;
  avgResolutionMinutes: number;
  byPriority: Metric[];
}
export interface Policy {
  id: number;
  priority: Priority;
  responseMinutes: number;
  resolutionMinutes: number;
}
export interface Settings {
  assignmentStrategy: string;
  priorityStrategy: string;
  p1Threshold: number;
  p2Threshold: number;
  p3Threshold: number;
  emailEnabled: boolean;
}
