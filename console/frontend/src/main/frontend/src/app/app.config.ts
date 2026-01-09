import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter, TitleStrategy, withHashLocation, withRouterConfig } from '@angular/router';
import { routes } from './app.routes';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { PagesTitleStrategy } from './pages-title-strategy';
import { httpInterceptorProviders } from './http-interceptors';
import { provideCharts, withDefaultRegisterables } from 'ng2-charts';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection(),
    provideRouter(
      routes,
      withHashLocation(),
      withRouterConfig({
        paramsInheritanceStrategy: 'always',
      }),
      // withDebugTracing(),
    ),
    provideHttpClient(withInterceptorsFromDi()),
    httpInterceptorProviders,
    provideCharts(withDefaultRegisterables()),
    { provide: TitleStrategy, useClass: PagesTitleStrategy },
    { provide: Window, useValue: globalThis },
  ],
};
