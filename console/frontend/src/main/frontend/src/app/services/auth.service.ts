import { inject, Injectable } from '@angular/core';
import { AppService } from '../app.service';
import { HttpClient } from '@angular/common/http';
import { combineLatest, map, Observable, of, tap } from 'rxjs';
import {
  Link,
  LinkName,
  SecurityItemsService,
  SecurityItems,
  SecurityRole,
  Links,
} from '../views/security-items/security-items.service';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly appService: AppService = inject(AppService);
  private readonly http: HttpClient = inject(HttpClient);
  private readonly securityItemsService: SecurityItemsService = inject(SecurityItemsService);

  private loggedIn = false;
  private allowedRoles: string[] = [];
  private allowedLinks: Link[] = [];
  private permissionsLoaded = false;

  setLoggedIn(username?: string): void {
    if (this.isNotAnonymous(username)) {
      this.loggedIn = true;
    }
  }

  private isNotAnonymous(username?: string): boolean {
    return !!username && username !== 'anonymous';
  }

  loadPermissions(): Observable<void> {
    if (this.permissionsLoaded) {
      return of();
    }

    return this.fetchPermissionData().pipe(
      tap(([securityItems, links]) => this.updatePermissions(securityItems, links)),
      map(() => {}),
    );
  }

  private fetchPermissionData(): Observable<[SecurityItems, Links]> {
    return combineLatest([
      this.securityItemsService.getSecurityItems(),
      this.securityItemsService.getEndpointsWithRoles(),
    ]);
  }

  private updatePermissions(securityItems: SecurityItems, links: Links): void {
    this.allowedRoles = this.filterAllowedRoles(securityItems.securityRoles);
    this.allowedLinks = this.filterAllowedLinks(links);
    this.permissionsLoaded = true;
  }

  private filterAllowedRoles(securityRoles: SecurityRole[]): string[] {
    return securityRoles.filter((role) => role.allowed).map((role) => role.name);
  }

  private filterAllowedLinks(links: Links): Link[] {
    return links.links.filter(
      (link) => !link.hasOwnProperty('roles') || link.roles.some((role) => this.allowedRoles.includes(role)),
    );
  }

  isLoggedIn(): boolean {
    return this.loggedIn;
  }

  hasAccessToLink(name: LinkName): boolean {
    return this.allowedLinks.some((link) => link.name === name);
  }

  logout(): Observable<object> {
    sessionStorage.clear();
    this.loggedIn = false;
    return this.http.get(`${this.appService.getServerPath()}iaf/api/logout`);
  }
}
