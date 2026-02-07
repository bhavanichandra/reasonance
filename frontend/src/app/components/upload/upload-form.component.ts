import { ChangeDetectionStrategy, Component, EventEmitter, input, Output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { FileRemoveEvent, FileSelectEvent, FileUploadModule } from 'primeng/fileupload';
import { SpaceService } from '../../services/space.service';
import { SpaceInitResult } from '../../models/space.model';
import { NgIcon } from '@ng-icons/core';

@Component({
  selector: 'app-upload-form',
  imports: [CommonModule, ButtonModule, FileUploadModule, NgIcon],
  templateUrl: './upload-form.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class: 'block w-full',
  },
})
export class UploadFormComponent {
  variant = input<'embedded' | 'page'>('embedded');

  @Output() success = new EventEmitter<SpaceInitResult>();
  @Output() failure = new EventEmitter<string>();

  selectedFiles = signal<File[]>([]);
  isDragging = signal(false);
  uploading = signal(false);
  errorMessage = signal<string | null>(null);

  constructor(private readonly spaceService: SpaceService) {}

  onDragOver(event: DragEvent) {
    event.preventDefault();
    this.isDragging.set(true);
  }

  onDragLeave(event: DragEvent) {
    event.preventDefault();
    this.isDragging.set(false);
  }

  onFilesSelected(event: FileSelectEvent) {
    this.isDragging.set(false);
    this.selectedFiles.set(event.currentFiles);
    this.errorMessage.set(null);
  }

  onFileRemoved(event: FileRemoveEvent) {
    this.selectedFiles.update((files) => files.filter((file) => file !== event.file));
  }

  onFilesCleared() {
    this.selectedFiles.set([]);
  }

  upload() {
    if (this.selectedFiles().length === 0 || this.uploading()) return;
    this.uploading.set(true);
    this.errorMessage.set(null);

    this.spaceService.createSpace(this.selectedFiles()).subscribe({
      next: (spaceRes) => {
        this.spaceService.initializeSpace(spaceRes.space.id).subscribe({
          next: (initRes) => {
            this.uploading.set(false);
            this.success.emit({
              spaceId: spaceRes.space.id,
              investigativeDirection: initRes.investigative_direction,
            });
          },
          error: (err) => {
            this.uploading.set(false);
            const msg = err?.error?.message || 'Could not initialize this space. Please try again.';
            this.errorMessage.set(msg);
            this.failure.emit(msg);
          },
        });
      },
      error: (err) => {
        this.uploading.set(false);
        const msg = err?.error?.message || 'Space creation failed. Please try again.';
        this.errorMessage.set(msg);
        this.failure.emit(msg);
      },
    });
  }

  removeFile(file: File, removeCb?: (event?: any, file?: any) => void) {
    // Keep PrimeNG internal list in sync if provided
    if (removeCb) {
      removeCb({ originalEvent: null }, file);
    }
    this.selectedFiles.update((files) => files.filter((f) => f !== file));
  }
}
