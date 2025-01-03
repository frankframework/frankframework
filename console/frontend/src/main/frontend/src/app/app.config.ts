import { ApplicationConfig } from '@angular/core';
import { provideRouter, TitleStrategy, withHashLocation, withRouterConfig } from '@angular/router';

import { routes } from './app.routes';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { PagesTitleStrategy } from './pages-title-strategy';
import { httpInterceptorProviders } from './http-interceptors';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(
      routes,
      withHashLocation(),
      withRouterConfig({
        paramsInheritanceStrategy: 'always',
      }),
    ),
    provideHttpClient(withInterceptorsFromDi()),
    httpInterceptorProviders,
    { provide: TitleStrategy, useClass: PagesTitleStrategy },
    { provide: Window, useValue: window },
  ],
};
