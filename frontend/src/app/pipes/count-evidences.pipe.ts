import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'countEvidences',
  standalone: true,
})
export class CountEvidencesPipe implements PipeTransform {
  transform(conclusions: any[]): number {
    if (!conclusions || !Array.isArray(conclusions)) return 0;
    return conclusions.reduce((count, conclusion) => {
      return count + (conclusion.evidences?.length || 0);
    }, 0);
  }
}
