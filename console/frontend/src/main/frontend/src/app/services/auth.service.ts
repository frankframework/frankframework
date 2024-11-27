import { Injectable } from '@angular/core';
import { AppService } from '../app.service';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private loggedIn = false;

  constructor(
    private http: HttpClient,
    private appService: AppService,
  ) {}

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

  setLoggedIn(username?: string): void {
    if (username && username != 'anonymous') {
      this.loggedIn = true;
    }
  }

  isLoggedIn(): boolean {
    return this.loggedIn;
  }

  logout(): Observable<object> {
    sessionStorage.clear();
    this.loggedIn = false;
    return this.http.get(`${this.appService.getServerPath()}iaf/api/logout`);
  }
}
