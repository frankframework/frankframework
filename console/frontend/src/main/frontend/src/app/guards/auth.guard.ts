import { CanActivateFn } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = (route) => {
  const authService = inject(AuthService);
  const linkName = route.routeConfig?.data?.['linkName'];

  if (!linkName) {
    return true;
  }

  return authService.hasAccessToLink(linkName);
};
