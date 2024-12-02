import { inject, Injectable } from '@angular/core';
import { AppService } from '../app.service';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Link, LinkName, SecurityItemsService } from '../views/security-items/security-items.service';

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

  setLoggedIn(username?: string): void {
    if (username && username !== 'anonymous') {
      this.loggedIn = true;
    }

    this.securityItemsService.getSecurityItems().subscribe({
      next: (data) => {
        this.allowedRoles = data.securityRoles
          .filter((role) => role.allowed) // Only include allowed roles
          .map((role) => role.name);
        this.securityItemsService.getEndpointsWithRoles().subscribe({
          next: ({ links }) => {
            this.allowedLinks = links.filter(
              (link) => !link.hasOwnProperty('roles') || link.roles.some((role) => this.allowedRoles.includes(role)),
            );
          },
        });
      },
    });
  }

  isLoggedIn(): boolean {
    return this.loggedIn;
  }

  hasAccessToLink(name: LinkName): boolean {
    console.log(this.allowedLinks);
    return this.allowedLinks.some((link) => link.name === name);
  }

  logout(): Observable<object> {
    sessionStorage.clear();
    this.loggedIn = false;
    return this.http.get(`${this.appService.getServerPath()}iaf/api/logout`);
  }
}
