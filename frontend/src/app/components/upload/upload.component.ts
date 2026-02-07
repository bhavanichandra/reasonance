import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { UploadFormComponent } from './upload-form.component';
import { SpaceInitResult } from '../../models/space.model';

@Component({
  selector: 'app-upload',
  imports: [RouterModule, CommonModule, ButtonModule, CardModule, UploadFormComponent],
  templateUrl: './upload.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UploadComponent {
  private readonly router = inject(Router);

  onUploadSuccess(result: SpaceInitResult) {
    this.router.navigate(['/board', result.spaceId], {
      state: { investigativeDirection: result.investigativeDirection },
    });
  }

  backHome() {
    this.router.navigate(['/spaces']);
  }
}
