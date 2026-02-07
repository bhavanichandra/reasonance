import { ChangeDetectionStrategy, Component, inject, OnDestroy, Renderer2, signal } from '@angular/core';
import { CommonModule, DOCUMENT } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { SpaceService } from '../../services/space.service';
import { Space, SpaceInitResult, SpaceStatus } from '../../models/space.model';
import { UploadFormComponent } from '../upload/upload-form.component';

import { TableModule } from 'primeng/table';

@Component({
  selector: 'app-spaces-list',
  imports: [CommonModule, RouterModule, ButtonModule, TableModule, UploadFormComponent],
  templateUrl: './spaces-list.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SpacesListComponent implements OnDestroy {
  private readonly spaceService = inject(SpaceService);
  private readonly router = inject(Router);
  private readonly document = inject(DOCUMENT);
  private readonly renderer = inject(Renderer2);
  spaces = signal<Space[]>([]);
  deletingIds = signal<Set<string>>(new Set());

  constructor() {
    // Defensive reset in case previous route locked body scrolling.
    this.renderer.removeClass(this.document.body, 'overflow-hidden');
    this.renderer.removeClass(this.document.body, 'h-screen');
    this.renderer.removeClass(this.document.body, 'min-h-0');
    this.renderer.addClass(this.document.body, 'overflow-auto');
    this.renderer.setStyle(this.document.body, 'overflow-y', 'auto');
    this.renderer.setStyle(this.document.body, 'height', 'auto');
    this.renderer.setStyle(this.document.body, 'min-height', '100vh');
    this.loadSpaces();
  }

  ngOnDestroy() {
    this.renderer.removeClass(this.document.body, 'overflow-auto');
    this.renderer.removeStyle(this.document.body, 'overflow-y');
    this.renderer.removeStyle(this.document.body, 'height');
    this.renderer.removeStyle(this.document.body, 'min-height');
  }

  navigateToSpace(space: Space) {
    if (space.status === SpaceStatus.CLOSED) {
      this.router.navigate(['/casefile', space.id]);
    } else {
      this.router.navigate(['/board', space.id]);
    }
  }

  createNewSpace() {
    this.router.navigate(['/upload']);
  }

  onUploadSuccess(result: SpaceInitResult) {
    this.router.navigate(['/board', result.spaceId], {
      state: { investigativeDirection: result.investigativeDirection },
    });
  }

  deleteSpace(space: Space) {
    const current = new Set(this.deletingIds());
    if (current.has(space.id)) return;
    current.add(space.id);
    this.deletingIds.set(current);

    this.spaceService.deleteSpace(space.id).subscribe({
      next: () => {
        this.loadSpaces();
        this.clearDeleting(space.id);
      },
      error: () => {
        this.clearDeleting(space.id);
      },
    });
  }

  isDeleting(spaceId: string): boolean {
    return this.deletingIds().has(spaceId);
  }

  private loadSpaces() {
    this.spaceService.listSpaces().subscribe({
      next: (spaces) => this.spaces.set(spaces),
      error: () => this.spaces.set([]),
    });
  }

  private clearDeleting(spaceId: string) {
    const next = new Set(this.deletingIds());
    next.delete(spaceId);
    this.deletingIds.set(next);
  }
}
