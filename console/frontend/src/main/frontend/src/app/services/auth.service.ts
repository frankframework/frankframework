import { inject, Injectable } from '@angular/core';
import { AppService } from '../app.service';
import { HttpClient } from '@angular/common/http';
import { combineLatest, map, Observable, tap, shareReplay } from 'rxjs';
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

  /* Currently not being used because servlet handles basic auth */
  /* login(username: string, password: string): void {
    if (username != 'anonymous') {
      this.authToken = this.Base64.encode(`${username}:${password}`);
      sessionStorage.setItem('authToken', this.authToken);
    }
    const location = sessionStorage.getItem('location') || 'status';
    const absUrl = window.location.href.split('login')[0];
    window.location.href = absUrl + location;
    window.location.reload();
  } */
  /* loggedin(): void {
    if (token != null && token != 'null') {
      if (this.router.url.includes('login'))
        this.router.navigateByUrl(
          sessionStorage.getItem('location') || 'status',
        );
    } else {
      if (this.consoleState.init > 0) {
        if (!this.router.url.includes('login'))
          sessionStorage.setItem('location', this.router.url || 'status');
        this.router.navigateByUrl('login');
      }
    }
  } */
  private loadingPermissionsPromise?: Promise<void>;

  setLoggedIn(username?: string): void {
    if (this.isNotAnonymous(username)) {
      this.loggedIn = true;
    }
  }

  private isNotAnonymous(username?: string): boolean {
    return !!username && username !== 'anonymous';
  }

  loadPermissions(): Promise<void> {
    if (this.loadingPermissionsPromise) {
      return this.loadingPermissionsPromise;
    }
    this.loadingPermissionsPromise = new Promise((resolve) => {
      combineLatest([
        this.securityItemsService.getSecurityItems(),
        this.securityItemsService.getEndpointsWithRoles(),
      ]).subscribe(([securityItems, links]) => {
        this.updatePermissions(securityItems, links);
        resolve();
      });
    });
    return this.loadingPermissionsPromise;
  }

  private updatePermissions(securityItems: SecurityItems, links: Links): void {
    this.allowedRoles = this.filterAllowedRoles(securityItems.securityRoles);
    this.allowedLinks = this.filterAllowedLinks(links);
  }

  private filterAllowedRoles(securityRoles: SecurityRole[]): string[] {
    return securityRoles.filter(({ allowed }) => allowed).map(({ name }) => name);
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
    this.securityItemsService.clearCache();
    this.loggedIn = false;
    return this.http.get(`${this.appService.getServerPath()}iaf/api/logout`);
  }
}
