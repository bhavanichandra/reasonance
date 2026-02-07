import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import {
  CreateSpaceResponse,
  InitializeResponse,
  ServerResult,
  Space,
} from '../models/space.model';
import { environment } from '../../environments/environment';

export interface SpaceDto extends Space {}

@Injectable({
  providedIn: 'root',
})
export class SpaceService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  /**
   * GET /api/v1/spaces
   * Fetch all spaces
   */
  listSpaces(): Observable<SpaceDto[]> {
    return this.http
      .get<ServerResult<SpaceDto[]>>(`${this.apiUrl}/spaces`)
      .pipe(map((result) => result.data));
  }

  /**
   * DELETE /api/v1/spaces/{spaceId}
   * Delete a space
   */
  deleteSpace(spaceId: string): Observable<void> {
    return this.http
      .delete<ServerResult<void>>(`${this.apiUrl}/spaces/${spaceId}`)
      .pipe(map(() => void 0));
  }

  /**
   * POST /api/v1/space
   * Create a new space with file(s)
   */
  createSpace(files: File[]): Observable<CreateSpaceResponse> {
    const formData = new FormData();
    files.forEach((file) => {
      formData.append('files', file);
    });
    return this.http
      .post<ServerResult<CreateSpaceResponse>>(`${this.apiUrl}/space`, formData)
      .pipe(map((result) => result.data));
  }

  /**
   * POST /api/v1/initialize
   * Initialize analysis for a space
   */
  initializeSpace(spaceId: string): Observable<InitializeResponse> {
    return this.http
      .post<ServerResult<InitializeResponse>>(`${this.apiUrl}/initialize`, { spaceId })
      .pipe(map((result) => result.data));
  }

  /**
   * POST /api/v1/space/{spaceId}/finalize
   * Finalize the space and generate final conclusion
   */
  finalizeSpace(spaceId: string): Observable<any> {
    return this.http
      .post<ServerResult<any>>(`${this.apiUrl}/space/${spaceId}/finalize`, {})
      .pipe(map((result) => result.data));
  }

  /**
   * GET /api/v1/space/{spaceId}/tree
   * Get reasoning tree for a space
   */
  getTree(spaceId: string): Observable<any> {
    return this.http
      .get<ServerResult<any>>(`${this.apiUrl}/space/${spaceId}/tree`)
      .pipe(map((result) => result.data));
  }

  /**
   * GET /api/v1/ping
   * Health check endpoint
   */
  ping(): Observable<any> {
    return this.http
      .get<ServerResult<any>>(`${this.apiUrl}/ping`)
      .pipe(map((result) => result.data));
  }
}
