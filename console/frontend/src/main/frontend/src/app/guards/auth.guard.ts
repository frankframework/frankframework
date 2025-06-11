import { CanActivateFn } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { LinkName } from '../views/security-items/security-items.service';

export const authGuard: CanActivateFn = async (route): Promise<boolean> => {
  const authService = inject(AuthService);
  const linkNames: LinkName | LinkName[] = route.routeConfig?.data?.['linkName'];

  if (!linkNames) {
    return true;
  }

  await authService.loadPermissions();
  return typeof linkNames === 'string'
    ? authService.hasAccessToLink(linkNames)
    : linkNames.some((linkName) => authService.hasAccessToLink(linkName));
};
