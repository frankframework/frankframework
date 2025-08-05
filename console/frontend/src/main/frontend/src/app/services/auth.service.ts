import { inject, Injectable } from '@angular/core';
import { AppService } from '../app.service';
import { HttpClient } from '@angular/common/http';
import { combineLatest, Observable } from 'rxjs';
import {
  Link,
  LinkName,
  Links,
  SecurityItems,
  SecurityItemsService,
  SecurityRole,
} from '../views/security-items/security-items.service';

type AllowedLinks = Pick<Link, 'name'> & Partial<Link>;

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private loggedIn = false;
  private allowedRoles: string[] = [];
  private allowedLinks: AllowedLinks[] = [];

  private readonly appService: AppService = inject(AppService);
  private readonly http: HttpClient = inject(HttpClient);
  private readonly securityItemsService: SecurityItemsService = inject(SecurityItemsService);
  private readonly onErrorAllowedLinks: AllowedLinks[] = [{ name: 'getFileContent' }, { name: 'getLogDirectory' }];

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

  loadPermissions(): Promise<void> {
    if (this.loadingPermissionsPromise) {
      return this.loadingPermissionsPromise;
    }
    this.loadingPermissionsPromise = new Promise((resolve) => {
      combineLatest([
        this.securityItemsService.getSecurityItems(),
        this.securityItemsService.getEndpointsWithRoles(),
      ]).subscribe({
        next: ([securityItems, links]) => {
          this.updatePermissions(securityItems, links);
          resolve();
        },
        error: (error) => {
          console.error("Couldn't load permissions", error);
          this.allowedLinks = this.onErrorAllowedLinks;
          resolve();
        },
      });
    });
    return this.loadingPermissionsPromise;
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

  private isNotAnonymous(username?: string): boolean {
    return !!username && username !== 'anonymous';
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
      (link) =>
        !Object.hasOwnProperty.call(link, 'roles') || link.roles.some((role) => this.allowedRoles.includes(role)),
    );
  }
}
