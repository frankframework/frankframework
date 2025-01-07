import { ApplicationConfig, importProvidersFrom } from '@angular/core';
import { provideRouter, TitleStrategy, withHashLocation, withRouterConfig } from '@angular/router';

import { routes } from './app.routes';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { PagesTitleStrategy } from './pages-title-strategy';
import { httpInterceptorProviders } from './http-interceptors';
import { provideCharts, withDefaultRegisterables } from 'ng2-charts';
import { NgIdleModule } from '@ng-idle/core';

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
    provideCharts(withDefaultRegisterables()),
    { provide: TitleStrategy, useClass: PagesTitleStrategy },
    { provide: Window, useValue: window },
    importProvidersFrom(NgIdleModule.forRoot()),
  ],
};
