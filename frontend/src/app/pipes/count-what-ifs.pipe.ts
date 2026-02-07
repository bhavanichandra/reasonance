import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'countWhatIfs',
  standalone: true,
})
export class CountWhatIfsPipe implements PipeTransform {
  transform(conclusions: any[]): number {
    if (!conclusions || !Array.isArray(conclusions)) return 0;
    return conclusions.filter((conclusion) => conclusion.whatIf).length;
  }
}
