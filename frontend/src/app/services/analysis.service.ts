import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import {
  AnalyzeResponse,
  BoardStateResponse,
  ConclusionProjection,
  CrossReferenceDto,
  EvidenceDto,
  FinalizeResponse,
  InsightDto,
  InvestigationSummary,
  Node,
  ServerResult,
  TurnDto,
  TreeProjection,
  TreeResponse,
} from '../models/space.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class AnalysisService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  /**
   * POST /api/v1/analyze
   * Analyze a conclusion against selected evidence
   */
  analyze(
    spaceId: string,
    conclusionId: string,
    evidenceIds: string[],
    turn?: number,
  ): Observable<AnalyzeResponse> {
    return this.http
      .post<ServerResult<AnalyzeResponse>>(`${this.apiUrl}/analyze`, {
        spaceId,
        conclusionId,
        evidenceIds,
        turn,
      })
      .pipe(map((result) => this.normalizeAnalyzeResponse(result.data)));
  }

  /**
   * GET /api/v1/space/{spaceId}/tree
   * Fetch the board tree representation
   */
  fetchTree(spaceId: string): Observable<TreeProjection> {
    return this.http.get<ServerResult<TreeResponse>>(`${this.apiUrl}/space/${spaceId}/tree`).pipe(
      map((result) => {
        const nodes = result.data.tree;
        const turn = result.data.currentTurn;
        const finalConclusion = result.data.finalConclusion;

        // Extract conclusions and other nodes
        const conclusionNodes = nodes.filter((node) => node.type === 'CONCLUSION');
        console.log('Filtered conclusion nodes:', conclusionNodes);

        const conclusions: ConclusionProjection[] = conclusionNodes
          .map((node) => ({
            id: node.id,
            content: node.content,
            summary: node.summary || undefined,
            turn: node.turn,
            locked: (node as any).locked || false,
            whatIf: (node as any).whatIf || undefined,
            evidences: (node as any).evidences || [],
            influencedConclusions: (node as any).influencedConclusions || [],
            tags: node.tags || [],
          }))
          .sort((a, b) => a.turn - b.turn);

        console.log('Mapped conclusions:', conclusions);

        // Extract available evidence from the "Unlinked Evidences" node
        const unlinkedNode = nodes.find((node) => node.content === 'Unlinked Evidences');
        let availableEvidence: Node[] = unlinkedNode ? (unlinkedNode as any).evidences || [] : [];

        // Filter out evidence already attached to conclusions
        const attachedEvidenceIds = new Set<string>();
        conclusions.forEach((conclusion) => {
          conclusion.evidences.forEach((ev) => {
            attachedEvidenceIds.add(ev.id);
          });
        });

        availableEvidence = availableEvidence.filter((ev) => !attachedEvidenceIds.has(ev.id));

        console.log('Tree fetched - nodes:', nodes);
        console.log('Extracted turn:', turn);
        console.log('Final conclusion:', finalConclusion);

        return {
          spaceId,
          turn,
          conclusions,
          availableEvidence,
          finalConclusion,
        };
      }),
    );
  }

  /**
   * GET /api/v1/space/{spaceId}/board-state
   * Canonical board state for turn-based workflow
   */
  fetchBoardState(spaceId: string): Observable<BoardStateResponse> {
    return this.http
      .get<ServerResult<BoardStateResponse>>(`${this.apiUrl}/space/${spaceId}/board-state`)
      .pipe(map((result) => this.normalizeBoardState(result.data)));
  }

  /**
   * POST /api/v1/space/{spaceId}/finalize
   * Finalize the analysis and generate final conclusion
   */
  finalize(spaceId: string): Observable<FinalizeResponse> {
    return this.http
      .post<ServerResult<FinalizeResponse>>(`${this.apiUrl}/space/${spaceId}/finalize`, {})
      .pipe(map((result) => this.normalizeFinalizeResponse(result.data)));
  }

  // Legacy method for backward compatibility
  getCaseFile(spaceId: string): Observable<TreeProjection> {
    return this.fetchTree(spaceId);
  }

  private normalizeAnalyzeResponse(data: AnalyzeResponse): AnalyzeResponse {
    const source = data as any;
    const crossReferences = this.normalizeCrossReferences(
      source.crossReferences ?? source.cross_references,
    );

    return {
      ...data,
      completedTurn: source.completedTurn ?? source.completed_turn ?? 0,
      nextTurn: source.nextTurn ?? source.next_turn ?? null,
      autoFinalized: source.autoFinalized ?? source.auto_finalized ?? false,
      finalConclusionId: source.finalConclusionId ?? source.final_conclusion_id ?? null,
      crossReferences,
      cross_references: crossReferences.map((item) => ({
        previous_node_id: item.previousNodeId,
        relationship: item.relationship,
        explanation: item.explanation,
      })),
      investigativeMomentum: source.investigativeMomentum ?? source.investigative_momentum,
      investigative_momentum: source.investigative_momentum ?? source.investigativeMomentum,
    };
  }

  private normalizeFinalizeResponse(data: FinalizeResponse): FinalizeResponse {
    const source = data as any;
    const summary = this.normalizeInvestigationSummary(
      source.investigationSummary ?? source.investigation_summary,
    );
    const finalConclusion = source.finalConclusion ?? source.final_conclusion;

    return {
      ...data,
      caseClosed: source.caseClosed ?? source.case_closed ?? false,
      case_closed: source.case_closed ?? source.caseClosed ?? false,
      investigationSummary: summary,
      investigation_summary: summary,
      finalConclusion,
      final_conclusion: finalConclusion,
      finalConclusionDetails: source.finalConclusionDetails ?? source.final_conclusion_details,
    };
  }

  private normalizeBoardState(data: BoardStateResponse): BoardStateResponse {
    const source = data as any;
    const turns: TurnDto[] = (source.turns ?? []).map((turn: any) => ({
      turn: turn.turn,
      status: turn.status,
    }));

    const inProgressRaw = source.inProgress ?? source.in_progress ?? {};
    const availableInsights: InsightDto[] = (
      inProgressRaw.availableInsights ??
      inProgressRaw.available_insights ??
      []
    ).map((insight: any) => ({
      id: insight.id,
      summary: insight.summary,
      content: insight.content,
      generatedTurn: insight.generatedTurn ?? insight.generated_turn ?? 0,
      state: insight.state,
      selectable: insight.selectable,
      tags: insight.tags ?? [],
      confidence: insight.confidence,
      reasoningType: insight.reasoningType ?? insight.reasoning_type,
      referencesTurns: this.normalizeReferencesTurns(
        insight.referencesTurns ?? insight.references_turns,
      ),
    }));

    const availableEvidences: EvidenceDto[] = (
      inProgressRaw.availableEvidences ??
      inProgressRaw.available_evidences ??
      []
    ).map((evidence: any) => ({
      id: evidence.id,
      summary: evidence.summary,
      content: evidence.content,
      generatedTurn: evidence.generatedTurn ?? evidence.generated_turn ?? 0,
      state: evidence.state,
      usedInTurn: evidence.usedInTurn ?? evidence.used_in_turn ?? null,
      draggable: evidence.draggable,
      tags: evidence.tags ?? [],
      relevance: evidence.relevance,
    }));

    const snapshotsByTurn: Record<string, any[]> =
      source.completedSnapshotsByTurn ?? source.completed_snapshots_by_turn ?? {};
    const normalizedSnapshots: Record<string, any[]> = {};
    Object.entries(snapshotsByTurn).forEach(([turn, snapshots]) => {
      normalizedSnapshots[turn] = (snapshots ?? []).map((snapshot: any) => ({
        ...snapshot,
        insight: {
          ...snapshot.insight,
          generatedTurn: snapshot.insight?.generatedTurn ?? snapshot.insight?.generated_turn ?? 0,
          confidence: snapshot.insight?.confidence,
        },
        evidences: (snapshot.evidences ?? []).map((evidence: any) => ({
          ...evidence,
          generatedTurn: evidence.generatedTurn ?? evidence.generated_turn ?? 0,
          relevance: evidence.relevance,
        })),
        whatIf: {
          ...snapshot.whatIf,
          generatedTurn: snapshot.whatIf?.generatedTurn ?? snapshot.whatIf?.generated_turn ?? 0,
          confidence: snapshot.whatIf?.confidence,
          reasoningType: snapshot.whatIf?.reasoningType ?? snapshot.whatIf?.reasoning_type,
          referencesTurns: this.normalizeReferencesTurns(
            snapshot.whatIf?.referencesTurns ?? snapshot.whatIf?.references_turns,
          ),
        },
      }));
    });

    return {
      ...data,
      spaceId: source.spaceId ?? source.space_id,
      currentTurn: source.currentTurn ?? source.current_turn ?? 1,
      maxTurns: source.maxTurns ?? source.max_turns ?? 5,
      finalized: source.finalized ?? false,
      turns,
      completedSnapshotsByTurn: normalizedSnapshots,
      inProgress: {
        availableInsights,
        availableEvidences,
      },
    };
  }

  private normalizeCrossReferences(value: unknown): CrossReferenceDto[] {
    if (!Array.isArray(value)) return [];
    return value.map((item: any) => ({
      previousNodeId: item.previousNodeId ?? item.previous_node_id ?? '',
      relationship: item.relationship ?? '',
      explanation: item.explanation ?? '',
    }));
  }

  private normalizeReferencesTurns(value: unknown): number[] {
    if (Array.isArray(value)) {
      return value.map(Number).filter((item) => Number.isFinite(item));
    }
    if (typeof value === 'string') {
      try {
        const parsed = JSON.parse(value);
        if (Array.isArray(parsed)) {
          return parsed.map(Number).filter((item) => Number.isFinite(item));
        }
      } catch {
        return [];
      }
    }
    return [];
  }

  private normalizeInvestigationSummary(value: any): InvestigationSummary {
    const summary = value ?? {};
    return {
      totalTurns: summary.totalTurns ?? summary.total_turns ?? 0,
      totalHypothesesExplored:
        summary.totalHypothesesExplored ?? summary.total_hypotheses_explored ?? 0,
      criticalEvidenceCount: summary.criticalEvidenceCount ?? summary.critical_evidence_count ?? 0,
      reasoningPath: summary.reasoningPath ?? summary.reasoning_path ?? '',
      decisiveMoments: summary.decisiveMoments ?? summary.decisive_moments ?? [],
      total_turns: summary.total_turns ?? summary.totalTurns ?? 0,
      total_hypotheses_explored:
        summary.total_hypotheses_explored ?? summary.totalHypothesesExplored ?? 0,
      critical_evidence_count:
        summary.critical_evidence_count ?? summary.criticalEvidenceCount ?? 0,
      reasoning_path: summary.reasoning_path ?? summary.reasoningPath ?? '',
      decisive_moments: summary.decisive_moments ?? summary.decisiveMoments ?? [],
    };
  }
}
