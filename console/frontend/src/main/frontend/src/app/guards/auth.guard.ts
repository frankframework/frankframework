import { CanActivateFn } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { lastValueFrom } from 'rxjs';

export const authGuard: CanActivateFn = async (route): Promise<boolean> => {
  const authService = inject(AuthService);
  const linkName = route.routeConfig?.data?.['linkName'];

  if (!linkName) {
    return true;
  }
  await lastValueFrom(authService.loadPermissions());
  return authService.hasAccessToLink(linkName);
};
