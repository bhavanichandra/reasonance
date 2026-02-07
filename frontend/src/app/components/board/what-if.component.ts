import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  input,
  signal,
  ViewChild,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgIcon } from '@ng-icons/core';
import { ButtonModule } from 'primeng/button';
import { Node } from '../../models/space.model';
import { animate } from 'animejs';

@Component({
  selector: 'app-what-if',
  imports: [CommonModule, NgIcon, ButtonModule],
  templateUrl: './what-if.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WhatIfComponent {
  node = input.required<Node>();
  @ViewChild('whatIfElement') whatIfElement?: ElementRef;

  showSummary = signal(true);

  toggleView() {
    if (this.whatIfElement?.nativeElement) {
      animate(this.whatIfElement.nativeElement, {
        scaleX: [1, 0.95, 1],
        opacity: [1, 0.7, 1],
        duration: 400,
        easing: 'easeInOutQuad',
        onComplete: () => {
          this.showSummary.update((val) => !val);
        },
      });
    }
  }
}
