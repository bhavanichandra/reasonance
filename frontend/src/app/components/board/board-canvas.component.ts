import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  OnDestroy,
  Renderer2,
  signal,
} from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { CommonModule, DOCUMENT } from '@angular/common';
import { CdkDragDrop, DragDropModule } from '@angular/cdk/drag-drop';
import { NgIcon } from '@ng-icons/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs';
import { AnalysisService } from '../../services/analysis.service';
import {
  BoardConclusionCard,
  BoardStateResponse,
  CompletedSnapshotDto,
  CrossReferenceDto,
  EvidenceDto,
  InsightDto,
  Space,
  TurnStatus,
} from '../../models/space.model';
import { SpaceService } from '../../services/space.service';
import { StickyNoteComponent } from './sticky-note.component';
import { EvidenceCardComponent } from './evidence-card.component';

@Component({
  selector: 'app-board-canvas',
  imports: [RouterModule, CommonModule, DragDropModule, NgIcon, StickyNoteComponent, EvidenceCardComponent],
  templateUrl: './board-canvas.component.html',
  host: { class: 'block h-screen overflow-hidden' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BoardCanvasComponent implements OnDestroy {
  private readonly route = inject(ActivatedRoute);
  protected readonly router = inject(Router);
  private readonly analysisService = inject(AnalysisService);
  private readonly spaceService = inject(SpaceService);
  private readonly document = inject(DOCUMENT);
  private readonly renderer = inject(Renderer2);
  private readonly spaceId$ = this.route.params.pipe(map((params) => params['id'] as string));
  private readonly spaces = toSignal(this.spaceService.listSpaces(), { initialValue: [] as Space[] });

  spaceId = toSignal(this.spaceId$, { initialValue: '' });
  boardState = signal<BoardStateResponse | null>(null);

  loadingBoardState = signal(false);
  loadingAnalyze = signal(false);
  finalizing = signal(false);
  analyzeError = signal<string | null>(null);

  activeTurn = signal(0);

  selectedInsightByTurn = signal<Map<number, string>>(new Map());
  pendingEvidenceByTurn = signal<Map<number, EvidenceDto[]>>(new Map());
  expandedInsightIds = signal<Set<string>>(new Set());
  lastAnalyzeCrossReferences = signal<CrossReferenceDto[]>([]);
  lastAnalyzeMomentum = signal<string | null>(null);

  turnTabs = computed(() => this.boardState()?.turns ?? []);
  currentTurn = computed(() => this.boardState()?.currentTurn ?? 1);
  maxTurns = computed(() => this.boardState()?.maxTurns ?? 5);
  isCaseFinalized = computed(() => !!this.boardState()?.finalized);

  selectedInsightId = computed(() => this.selectedInsightByTurn().get(this.activeTurn()));
  pendingEvidenceForActiveTurn = computed(() => this.pendingEvidenceByTurn().get(this.activeTurn()) ?? []);

  allInsights = computed(() => {
    if (!this.isTurnInProgress(this.activeTurn())) return [];
    return this.boardState()?.inProgress?.availableInsights ?? [];
  });

  availableInsights = computed(() => {
    if (!this.isTurnInProgress(this.activeTurn())) return [];
    const selectedId = this.selectedInsightId();
    return selectedId
      ? this.allInsights().filter((item) => item.id !== selectedId)
      : this.allInsights();
  });

  availableEvidences = computed(() => {
    if (!this.isTurnInProgress(this.activeTurn())) return [];
    const all = this.boardState()?.inProgress?.availableEvidences ?? [];
    const usedIds = new Set(this.pendingEvidenceForActiveTurn().map((item) => item.id));
    return all.filter((item) => !usedIds.has(item.id));
  });

  selectedInsight = computed(() => {
    const selectedId = this.selectedInsightId();
    if (!selectedId) return null;
    return this.allInsights().find((item) => item.id === selectedId) ?? null;
  });

  selectedInsightConfidence = computed(() => {
    const confidence = this.selectedInsight()?.confidence;
    if (!confidence) return null;
    return String(confidence).toUpperCase();
  });

  selectedInsightReasoningType = computed(() => {
    const reasoningType = this.selectedInsight()?.reasoningType;
    if (!reasoningType) return null;
    return String(reasoningType).toUpperCase();
  });

  selectedInsightCard = computed<BoardConclusionCard | null>(() => {
    const insight = this.selectedInsight();
    if (!insight) return null;
    return {
      id: insight.id,
      summary: insight.summary,
      content: insight.content,
      turn: this.activeTurn(),
      locked: false,
      tags: insight.tags,
      confidence: insight.confidence,
      evidences: [],
    };
  });

  completedSnapshotsForActiveTurn = computed(() => this.getCompletedSnapshotsForTurn(this.activeTurn()));
  hasAnyCompletedTurns = computed(() => {
    const byTurn = this.boardState()?.completedSnapshotsByTurn;
    if (!byTurn) return false;
    return Object.values(byTurn).some((list) => Array.isArray(list) && list.length > 0);
  });

  canAnalyze = computed(() => {
    return (
      this.isTurnInProgress(this.activeTurn()) &&
      !!this.selectedInsightId() &&
      this.pendingEvidenceForActiveTurn().length > 0 &&
      !this.loadingAnalyze()
    );
  });

  isBusy = computed(() => this.loadingBoardState() || this.loadingAnalyze() || this.finalizing());

  busyMessage = computed(() => {
    if (this.loadingAnalyze()) return 'Sherlock is thinking abductively...';
    if (this.finalizing()) return 'Sherlock is synthesizing the investigation...';
    return 'Sherlock is preparing the board...';
  });

  spaceName = computed(() => {
    const id = this.spaceId();
    const current = this.spaces().find((space) => space.id === id);
    return current?.name || id || 'Unknown Space';
  });

  constructor() {
    this.renderer.removeStyle(this.document.body, 'overflow-y');
    this.renderer.removeStyle(this.document.body, 'height');
    this.renderer.removeStyle(this.document.body, 'min-height');

    this.renderer.addClass(this.document.body, 'overflow-hidden');
    this.renderer.addClass(this.document.body, 'h-screen');
    this.renderer.addClass(this.document.body, 'min-h-0');

    effect((onCleanup) => {
      const currentSpaceId = this.spaceId();
      if (!currentSpaceId) return;
      const sub = this.refreshBoardState(currentSpaceId);
      onCleanup(() => sub.unsubscribe());
    });
  }

  ngOnDestroy() {
    this.renderer.removeClass(this.document.body, 'overflow-hidden');
    this.renderer.removeClass(this.document.body, 'h-screen');
    this.renderer.removeClass(this.document.body, 'min-h-0');
  }

  isTurnInProgress(turn: number): boolean {
    const status = this.turnTabs().find((item) => item.turn === turn)?.status;
    return status === TurnStatus.IN_PROGRESS || status === 'IN_PROGRESS';
  }

  isTurnCompleted(turn: number): boolean {
    const status = this.turnTabs().find((item) => item.turn === turn)?.status;
    return status === TurnStatus.COMPLETED || status === 'COMPLETED';
  }

  isTurnActive(turn: number): boolean {
    return this.activeTurn() === turn;
  }

  onCanvasDropped(event: CdkDragDrop<unknown>) {
    if (!this.isTurnInProgress(this.activeTurn())) return;
    if (event.previousContainer === event.container) return;

    if (event.previousContainer.id === 'insightSourceList') {
      if (this.selectedInsightId()) return;
      const dragged = event.item.data as InsightDto | undefined;
      if (!dragged?.id) return;

      const existsInPool = this.availableInsights().some((item) => item.id === dragged.id);
      if (!existsInPool) return;

      const current = new Map(this.selectedInsightByTurn());
      current.set(this.activeTurn(), dragged.id);
      this.selectedInsightByTurn.set(current);

      const pending = new Map(this.pendingEvidenceByTurn());
      pending.set(this.activeTurn(), []);
      this.pendingEvidenceByTurn.set(pending);
      this.lastAnalyzeCrossReferences.set([]);
      this.lastAnalyzeMomentum.set(null);
      this.analyzeError.set(null);
      return;
    }

    if (event.previousContainer.id === 'evidenceSourceList') {
      const selectedId = this.selectedInsightId();
      const draggedEvidence = event.item.data as EvidenceDto | undefined;
      if (!selectedId || !draggedEvidence?.id) return;
      this.attachEvidence(selectedId, draggedEvidence.id);
    }
  }

  clearSelectedInsight() {
    const turn = this.activeTurn();
    const selected = new Map(this.selectedInsightByTurn());
    selected.delete(turn);
    this.selectedInsightByTurn.set(selected);

    const pending = new Map(this.pendingEvidenceByTurn());
    pending.delete(turn);
    this.pendingEvidenceByTurn.set(pending);
    this.analyzeError.set(null);
    this.lastAnalyzeCrossReferences.set([]);
    this.lastAnalyzeMomentum.set(null);
  }

  toggleInsightExpanded(insightId: string, event?: MouseEvent) {
    event?.stopPropagation();
    const current = new Set(this.expandedInsightIds());
    if (current.has(insightId)) {
      current.delete(insightId);
    } else {
      current.add(insightId);
    }
    this.expandedInsightIds.set(current);
  }

  isInsightExpanded(insightId: string): boolean {
    return this.expandedInsightIds().has(insightId);
  }

  attachEvidence(conclusionId: string, evidenceId: string) {
    if (!this.isTurnInProgress(this.activeTurn())) return;
    if (conclusionId !== this.selectedInsightId()) return;

    const evidence = this.availableEvidences().find((item) => item.id === evidenceId);
    if (!evidence) return;

    const mapByTurn = new Map(this.pendingEvidenceByTurn());
    const existing = mapByTurn.get(this.activeTurn()) ?? [];
    if (existing.some((item) => item.id === evidence.id)) return;

    mapByTurn.set(this.activeTurn(), [...existing, evidence]);
    this.pendingEvidenceByTurn.set(mapByTurn);
    this.analyzeError.set(null);
  }

  removePendingEvidence(evidenceId: string) {
    if (!this.isTurnInProgress(this.activeTurn())) return;

    const mapByTurn = new Map(this.pendingEvidenceByTurn());
    const existing = mapByTurn.get(this.activeTurn()) ?? [];
    const next = existing.filter((item) => item.id !== evidenceId);
    mapByTurn.set(this.activeTurn(), next);
    this.pendingEvidenceByTurn.set(mapByTurn);
  }

  onAnalyse() {
    const state = this.boardState();
    const selectedInsightId = this.selectedInsightId();
    if (!state || !selectedInsightId || !this.canAnalyze()) return;

    this.loadingAnalyze.set(true);
    this.analyzeError.set(null);

    const evidenceIds = this.pendingEvidenceForActiveTurn().map((item) => item.id);
    const analyzingTurn = this.activeTurn();

    this.analysisService
      .analyze(state.spaceId, selectedInsightId, evidenceIds, analyzingTurn)
      .subscribe({
        next: (response) => {
          this.loadingAnalyze.set(false);

          const turnToClear = response.completedTurn || analyzingTurn;

          const selected = new Map(this.selectedInsightByTurn());
          selected.delete(turnToClear);
          this.selectedInsightByTurn.set(selected);

          const pending = new Map(this.pendingEvidenceByTurn());
          pending.delete(turnToClear);
          this.pendingEvidenceByTurn.set(pending);
          this.lastAnalyzeCrossReferences.set(response.crossReferences ?? []);
          this.lastAnalyzeMomentum.set(response.investigativeMomentum ?? null);

          if (response.autoFinalized) {
            this.router.navigate(['/casefile', state.spaceId]);
            return;
          }

          this.refreshBoardState(state.spaceId);
        },
        error: (err) => {
          this.loadingAnalyze.set(false);
          this.analyzeError.set(this.mapDomainError(err));
        },
      });
  }

  onFinalize() {
    const state = this.boardState();
    if (!state) return;

    this.finalizing.set(true);
    this.analysisService.finalize(state.spaceId).subscribe({
      next: (response) => {
        this.finalizing.set(false);
        this.router.navigate(['/casefile', state.spaceId], {
          state: {
            investigationSummary: response.investigationSummary ?? response.investigation_summary,
            finalConclusionDetails: response.finalConclusionDetails,
          },
        });
      },
      error: (err) => {
        this.finalizing.set(false);
        this.analyzeError.set(this.mapDomainError(err));
      },
    });
  }

  retryLoad() {
    const stateSpaceId = this.spaceId();
    if (!stateSpaceId) return;
    this.refreshBoardState(stateSpaceId);
  }

  private getCompletedSnapshotsForTurn(turn: number): CompletedSnapshotDto[] {
    const byTurn = this.boardState()?.completedSnapshotsByTurn;
    if (!byTurn) return [];
    return byTurn[String(turn)] || [];
  }

  private refreshBoardState(spaceId: string) {
    this.loadingBoardState.set(true);
    return this.analysisService.fetchBoardState(spaceId).subscribe({
      next: (state) => {
        this.loadingBoardState.set(false);
        this.boardState.set(state);

        if (state.finalized) {
          this.router.navigate(['/casefile', state.spaceId]);
          return;
        }

        const turns = state.turns?.map((item) => item.turn) ?? [];
        const current = this.activeTurn();
        if (turns.length === 0) {
          this.activeTurn.set(state.currentTurn || 1);
          return;
        }

        if (!current || !turns.includes(current)) {
          this.activeTurn.set(state.currentTurn || turns[turns.length - 1]);
        }
      },
      error: (err) => {
        this.loadingBoardState.set(false);
        this.analyzeError.set(this.mapDomainError(err));
      },
    });
  }

  private mapDomainError(err: any): string {
    const code = err?.error?.code as string | undefined;
    if (!code) return 'Something went wrong. Please try again.';

    switch (code) {
      case 'INVALID_TURN':
        return 'This turn is no longer valid. Refresh and try again.';
      case 'INSIGHT_NOT_AVAILABLE':
        return 'That insight is no longer available.';
      case 'EVIDENCE_NOT_AVAILABLE':
        return 'One or more evidence items are no longer available.';
      case 'MAX_TURNS_REACHED':
        return 'Maximum turns reached. Finalize the case.';
      default:
        return 'Action failed. Please retry.';
    }
  }
}
