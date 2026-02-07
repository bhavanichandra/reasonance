import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  input,
  Output,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { CdkDragDrop, DragDropModule } from '@angular/cdk/drag-drop';
import { BoardConclusionCard, EvidenceDto, Node } from '../../models/space.model';
import { WhatIfComponent } from './what-if.component';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { AccordionModule } from 'primeng/accordion';

@Component({
  selector: 'app-sticky-note',
  imports: [CommonModule, DragDropModule, WhatIfComponent, ButtonModule, DialogModule, AccordionModule],
  templateUrl: './sticky-note.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StickyNoteComponent {
  conclusion = input.required<BoardConclusionCard>();
  pendingEvidenceCount = input(0);
  totalPendingEvidenceCount = input(0);
  isActiveTurn = input(false);
  readonly = input(false);
  isSelectedInsight = input(false);
  acceptEvidenceDrop = input(false);
  showEvidencePanel = input(true);
  pendingEvidenceItems = input<EvidenceDto[]>([]);
  @Output() evidenceDropped = new EventEmitter<string>();

  evidenceDialogOpen = signal(false);
  selectedEvidence = signal<{ id: string; summary?: string; content: string } | null>(null);
  allEvidenceDialogOpen = signal(false);
  activeEvidenceId = signal<string | number | string[] | number[] | null | undefined>(null);

  onDrop(event: CdkDragDrop<unknown>) {
    if (this.readonly() || this.conclusion().locked) return;
    if (!this.isSelectedInsight() || !this.acceptEvidenceDrop()) return;
    if (event.previousContainer === event.container) return;
    if (event.previousContainer.id !== 'evidenceSourceList') return;

    // If there's already pending evidence on another conclusion, reject this drop
    if (this.totalPendingEvidenceCount() > 0 && this.pendingEvidenceCount() === 0) {
      return;
    }

    const evidenceNode = event.item.data as Node;
    this.evidenceDropped.emit(evidenceNode.id);
  }

  openEvidenceDialog(evidence: { id: string; summary?: string; content: string }) {
    this.selectedEvidence.set(evidence);
    this.evidenceDialogOpen.set(true);
  }

  openAllEvidenceDialog() {
    this.allEvidenceDialogOpen.set(true);
  }

  closeEvidenceDialog() {
    this.evidenceDialogOpen.set(false);
    this.selectedEvidence.set(null);
  }
}
