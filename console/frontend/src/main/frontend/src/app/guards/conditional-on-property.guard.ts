import { CanActivateFn } from '@angular/router';
import { inject } from '@angular/core';
import { AppService } from '../app.service';

export const conditionalOnPropertyGuard: CanActivateFn = (route) => {
  const appService = inject(AppService);
  const appConstants = appService.APP_CONSTANTS;
  const propertyName: string = route.routeConfig?.data?.['onProperty'];
  return appConstants[propertyName] === 'true';
};
