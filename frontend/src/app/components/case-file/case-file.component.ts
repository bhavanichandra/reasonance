import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { NgIcon } from '@ng-icons/core';
import { AnalysisService } from '../../services/analysis.service';
import { FinalConclusionDetails, InvestigationSummary, TreeProjection, Node } from '../../models/space.model';
import { CountEvidencesPipe } from '../../pipes/count-evidences.pipe';
import { CountWhatIfsPipe } from '../../pipes/count-what-ifs.pipe';
import { toSignal } from '@angular/core/rxjs-interop';
import { map, of, switchMap } from 'rxjs';

@Component({
  selector: 'app-case-file',
  imports: [RouterModule, CommonModule, CountEvidencesPipe, CountWhatIfsPipe, ButtonModule, NgIcon, DialogModule],
  templateUrl: './case-file.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CaseFileComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly analysisService = inject(AnalysisService);
  private readonly spaceId$ = this.route.params.pipe(map((params) => params['id'] as string));
  spaceId = toSignal(this.spaceId$, { initialValue: '' });
  caseFile = toSignal(
    this.spaceId$.pipe(
      switchMap((spaceId) => (spaceId ? this.analysisService.getCaseFile(spaceId) : of(null))),
    ),
    { initialValue: null as TreeProjection | null },
  );
  investigationSummary = signal<InvestigationSummary | null>(
    history.state?.investigationSummary ?? null,
  );
  finalConclusionDetails = signal<FinalConclusionDetails | null>(
    history.state?.finalConclusionDetails ?? null,
  );

  selectedSignal = signal<Node | null>(null);
  displaySignalDialog = signal(false);

  viewSignal(signal: Node) {
    this.selectedSignal.set(signal);
    this.displaySignalDialog.set(true);
  }

  hasBackendSummary(summary: InvestigationSummary | null): boolean {
    if (!summary) return false;
    const turns = summary.totalTurns ?? summary.total_turns ?? 0;
    const hypotheses = summary.totalHypothesesExplored ?? summary.total_hypotheses_explored ?? 0;
    const critical = summary.criticalEvidenceCount ?? summary.critical_evidence_count ?? 0;
    const path = (summary.reasoningPath ?? summary.reasoning_path ?? '').trim();
    return turns > 0 || hypotheses > 0 || critical > 0 || path.length > 0;
  }

  summaryTotalTurns(summary: InvestigationSummary | null, file: TreeProjection): number {
    if (!summary) return file.turn;
    const value = summary.totalTurns ?? summary.total_turns ?? 0;
    return value > 0 ? value : file.turn;
  }

  summaryTotalHypotheses(summary: InvestigationSummary | null, file: TreeProjection): number {
    const fallback = file.conclusions.filter((item) => !!item.whatIf).length;
    if (!summary) return fallback;
    const value = summary.totalHypothesesExplored ?? summary.total_hypotheses_explored ?? 0;
    return value > 0 ? value : fallback;
  }

  summaryCriticalCount(summary: InvestigationSummary | null, file: TreeProjection): number {
    const evidenceIds = new Set<string>();
    file.conclusions.forEach((conclusion) => {
      conclusion.evidences.forEach((ev) => evidenceIds.add(ev.id));
    });
    const fallback = evidenceIds.size;
    if (!summary) return fallback;
    const value = summary.criticalEvidenceCount ?? summary.critical_evidence_count ?? 0;
    return value > 0 ? value : fallback;
  }

  summaryReasoningPath(summary: InvestigationSummary | null, file: TreeProjection): string {
    const fallback = `The investigation progressed across ${file.turn} turns, exploring ${file.conclusions.filter((item) => !!item.whatIf).length} key hypotheses and linking ${file.conclusions.reduce((acc, item) => acc + item.evidences.length, 0)} signals to build the final interpretive model.`;
    if (!summary) return fallback;
    const value = summary.reasoningPath ?? summary.reasoning_path ?? '';
    return value.trim().length > 0 ? value : fallback;
  }

  summaryDecisiveMoments(summary: InvestigationSummary | null, file: TreeProjection) {
    if (summary) {
      const moments = summary.decisiveMoments ?? summary.decisive_moments ?? [];
      if (moments.length > 0) return moments;
    }
    if (file.conclusions.length === 0) return [];
    const firstTurn = Math.min(...file.conclusions.map((c) => c.turn));
    const lastTurn = Math.max(...file.conclusions.map((c) => c.turn));
    return [
      { turn: firstTurn, moment: 'Initial insight framing established the investigative direction.' },
      { turn: lastTurn, moment: 'Final synthesis consolidated the strongest explanatory pattern.' },
    ];
  }

  finalConfidence(file: TreeProjection): string | null {
    return this.finalConclusionDetails()?.confidence ?? (file.finalConclusion as any)?.confidence ?? null;
  }

  finalSupportingTurns(): number[] {
    return this.finalConclusionDetails()?.supporting_turns ?? [];
  }

  finalKeyBreakthroughTurn(): number | null {
    return this.finalConclusionDetails()?.key_breakthrough_turn ?? null;
  }

  finalAlternativeTheories() {
    return this.finalConclusionDetails()?.alternative_theories_ruled_out ?? [];
  }
}
