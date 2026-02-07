import { Routes } from '@angular/router';
import { SpacesListComponent } from './components/spaces-list/spaces-list.component';
import { UploadComponent } from './components/upload/upload.component';
import { BoardCanvasComponent } from './components/board/board-canvas.component';
import { CaseFileComponent } from './components/case-file/case-file.component';

export const routes: Routes = [
  { path: '', redirectTo: 'spaces', pathMatch: 'full' },
  { path: 'spaces', component: SpacesListComponent },
  { path: 'upload', component: UploadComponent },
  { path: 'board/:id', component: BoardCanvasComponent },
  { path: 'casefile/:id', component: CaseFileComponent },
  { path: '**', redirectTo: 'spaces' },
];
