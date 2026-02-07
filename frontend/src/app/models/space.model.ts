export type NodeType =
  | 'EVIDENCE'
  | 'CONCLUSION'
  | 'WHAT_IF'
  | 'FINAL_CONCLUSION'
  | 'SOURCE_SUMMARY';

export enum SpaceStatus {
  NEW = 'NEW',
  IN_PROGRESS = 'IN_PROGRESS',
  CLOSED = 'CLOSED',
}

export enum TurnStatus {
  IN_PROGRESS = 'IN_PROGRESS',
  COMPLETED = 'COMPLETED',
  LOCKED = 'LOCKED',
}

export enum ConclusionState {
  AVAILABLE = 'AVAILABLE',
  SELECTED = 'SELECTED',
  COMPLETED = 'COMPLETED',
  LOCKED = 'LOCKED',
}

export enum EvidenceState {
  AVAILABLE = 'AVAILABLE',
  USED = 'USED',
  LOCKED = 'LOCKED',
}

export type ConfidenceLevel = 'low' | 'medium' | 'high' | 'moderate' | 'definitive';
export type RelevanceLevel = 'supporting' | 'important' | 'critical';
export type ReasoningType = 'extension' | 'contradiction' | 'synthesis' | 'pivot';

// Core Models
export interface Node {
  id: string;
  type: NodeType;
  content: string;
  summary?: string;
  turn: number;
  space: string;
  tags?: string[];
  confidence?: ConfidenceLevel | string;
  relevance?: RelevanceLevel | string;
  reasoningType?: ReasoningType | string;
  referencesTurns?: number[];
}

export interface Space {
  id: string;
  name: string;
  createdAt: string;
  currentTurn: number;
  status: SpaceStatus;
}

export interface Document {
  id: string;
  name: string;
  key: string;
  createdAt: string;
  space: string;
}

// Response Models
export interface CreateSpaceResponse {
  space: Space;
  documents: Document[];
}

export interface SpaceInitResult {
  spaceId: string;
  investigativeDirection?: string;
}

export interface InitializeResponse {
  spaceId: string;
  evidenceIds: string[];
  conclusionIds: string[];
  investigative_direction?: string;
}

export interface CrossReference {
  previous_node_id: string;
  relationship: string;
  explanation: string;
}

export interface CrossReferenceDto {
  previousNodeId: string;
  relationship: 'supports' | 'contradicts' | 'extends' | 'replaces' | string;
  explanation: string;
}

export interface AnalyzeResponse {
  success: boolean;
  completedTurn: number;
  nextTurn?: number | null;
  autoFinalized: boolean;
  snapshot?: CompletedSnapshotDto;
  finalConclusionId?: string | null;
  // Backward compatible optional fields
  whatIf?: Node;
  evidenceIds?: string[];
  conclusionIds?: string[];
  cross_references?: CrossReference[];
  crossReferences?: CrossReferenceDto[];
  investigative_momentum?: string;
  investigativeMomentum?: string;
}

export interface TreeResponse {
  tree: Node[];
  currentTurn: number;
  finalConclusion?: Node;
}

export interface PingResponse {
  status: string;
  now: string;
}

export interface DecisiveMoment {
  turn: number;
  moment: string;
}

export interface InvestigationSummary {
  totalTurns?: number;
  totalHypothesesExplored?: number;
  criticalEvidenceCount?: number;
  reasoningPath?: string;
  decisiveMoments?: DecisiveMoment[];
  total_turns: number;
  total_hypotheses_explored: number;
  critical_evidence_count: number;
  reasoning_path: string;
  decisive_moments: DecisiveMoment[];
}

export interface AlternativeTheory {
  theory: string;
  reasonExcluded?: string;
  reason_excluded?: string;
}

export interface FinalConclusionDetails {
  final_conclusion_content?: string;
  final_conclusion_summary?: string;
  tags?: string[];
  confidence?: ConfidenceLevel | string;
  key_breakthrough_turn?: number;
  supporting_turns?: number[];
  alternative_theories_ruled_out?: AlternativeTheory[];
}

export interface FinalizeResponse {
  case_closed: boolean;
  caseClosed?: boolean;
  investigation_summary: InvestigationSummary;
  investigationSummary?: InvestigationSummary;
  final_conclusion: Node;
  finalConclusion?: Node;
  finalConclusionDetails?: FinalConclusionDetails;
}

// Board-specific Models
export interface NodeDto {
  id: string;
  type: NodeType;
  content: string;
  summary?: string;
  turn: number;
  locked: boolean;
  children?: NodeDto[];
  parentWhatIfId?: string;
  tags?: string[];
}

export interface BoardNodeDto {
  id: string;
  type: NodeType;
  content: string;
  summary?: string;
  turn: number;
  locked: boolean;
  whatIf?: NodeDto;
  evidences: NodeDto[];
  influencedConclusions: NodeDto[];
  tags?: string[];
}

// Server Result Wrapper
export interface ServerResult<T> {
  success: boolean;
  message: string;
  data: T;
}

export interface ConclusionProjection {
  id: string;
  content: string;
  summary?: string;
  turn: number;
  locked: boolean;
  whatIf?: Node;
  evidences: Node[];
  influencedConclusions: string[];
  tags?: string[];
  confidence?: ConfidenceLevel | string;
}

export interface TreeProjection {
  spaceId: string;
  turn: number;
  conclusions: ConclusionProjection[];
  availableEvidence: Node[];
  finalConclusion?: Node;
}

// Board-state DTOs
export interface TurnDto {
  turn: number;
  status: TurnStatus | string;
}

export interface InsightDto {
  id: string;
  summary?: string;
  content: string;
  generatedTurn: number;
  state?: ConclusionState | string;
  selectable?: boolean;
  tags?: string[];
  confidence?: ConfidenceLevel | string;
  reasoningType?: ReasoningType | string;
  referencesTurns?: number[];
}

export interface EvidenceDto {
  id: string;
  summary?: string;
  content: string;
  generatedTurn: number;
  state?: EvidenceState | string;
  usedInTurn?: number | null;
  draggable?: boolean;
  tags?: string[];
  relevance?: RelevanceLevel | string;
}

export interface InsightSnapshotDto {
  id: string;
  summary?: string;
  content: string;
  generatedTurn: number;
  confidence?: ConfidenceLevel | string;
}

export interface EvidenceSnapshotDto {
  id: string;
  summary?: string;
  content: string;
  generatedTurn: number;
  relevance?: RelevanceLevel | string;
}

export interface WhatIfSnapshotDto {
  id: string;
  summary?: string;
  content: string;
  generatedTurn: number;
  confidence?: ConfidenceLevel | string;
  reasoningType?: ReasoningType | string;
  referencesTurns?: number[];
}

export interface CompletedSnapshotDto {
  turn: number;
  insight: InsightSnapshotDto;
  evidences: EvidenceSnapshotDto[];
  whatIf: WhatIfSnapshotDto;
}

export interface InProgressDto {
  availableInsights: InsightDto[];
  availableEvidences: EvidenceDto[];
}

export interface BoardStateResponse {
  spaceId: string;
  currentTurn: number;
  maxTurns: number;
  finalized: boolean;
  turns: TurnDto[];
  completedSnapshotsByTurn: Record<string, CompletedSnapshotDto[]>;
  inProgress: InProgressDto;
}

// UI helper type for sticky-note rendering
export interface BoardConclusionCard {
  id: string;
  summary?: string;
  content: string;
  turn: number;
  locked: boolean;
  tags?: string[];
  confidence?: ConfidenceLevel | string;
  evidences: Array<{ id: string; summary?: string; content: string }>;
  whatIf?: Node;
}
