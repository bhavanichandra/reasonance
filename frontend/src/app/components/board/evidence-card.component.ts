import {
  ChangeDetectionStrategy,
  Component,
  input,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { DragDropModule } from '@angular/cdk/drag-drop';
import { NgIcon } from '@ng-icons/core';
import { EvidenceDto } from '../../models/space.model';

@Component({
  selector: 'app-evidence-card',
  imports: [CommonModule, DragDropModule, NgIcon],
  templateUrl: './evidence-card.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EvidenceCardComponent {
  node = input.required<EvidenceDto>();
  draggableDisabled = input(false);
  expanded = signal(false);

  toggleExpanded(event?: MouseEvent) {
    event?.stopPropagation();
    this.expanded.update((current) => !current);
  }
}
