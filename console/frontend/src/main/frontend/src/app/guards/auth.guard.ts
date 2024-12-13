import { CanActivateFn } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = async (route): Promise<boolean> => {
  const authService = inject(AuthService);
  const linkName = route.routeConfig?.data?.['linkName'];

  if (!linkName) {
    return true;
  }
  await authService.loadPermissions();
  return authService.hasAccessToLink(linkName);
};
