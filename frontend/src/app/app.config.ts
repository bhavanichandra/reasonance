import { ApplicationConfig, provideZonelessChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { providePrimeNG } from 'primeng/config';
import Aura from '@primeuix/themes/aura';
import { provideIcons } from '@ng-icons/core';
import {
  phosphorArrowRight,
  phosphorArrowSquareOut,
  phosphorBrain,
  phosphorCaretDown,
  phosphorCheck,
  phosphorClock,
  phosphorEye,
  phosphorFileText,
  phosphorHouse,
  phosphorMagnifyingGlass,
  phosphorNotebook,
  phosphorPlus,
  phosphorTrash,
  phosphorX,
} from '@ng-icons/phosphor-icons/regular';

import { phosphorEyesDuotone } from '@ng-icons/phosphor-icons/duotone';

import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZonelessChangeDetection(),
    provideRouter(routes),
    provideHttpClient(),
    providePrimeNG({
      theme: {
        preset: Aura,
        options: {
          darkModeSelector: '.my-app-dark',
        },
      },
    }),
    provideIcons({
      phosphorPlus,
      phosphorMagnifyingGlass,
      phosphorCheck,
      phosphorArrowRight,
      phosphorArrowSquareOut,
      phosphorHouse,
      phosphorTrash,
      phosphorClock,
      phosphorFileText,
      phosphorCaretDown,
      phosphorNotebook,
      phosphorBrain,
      phosphorEye,
      phosphorEyesDuotone,
      phosphorX,
    }),
  ],
};
