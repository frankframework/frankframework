import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { Base64Service } from './base64.service';
import { AppConstants, AppService, ConsoleState } from '../app.service';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private authToken?: string;
  private consoleState: ConsoleState;

  constructor(
    private http: HttpClient,
    private router: Router,
    private appService: AppService,
    private Base64: Base64Service,
  ) {
    this.consoleState = this.appService.CONSOLE_STATE;
  }

  login(username: string, password: string): void {
    if (username != 'anonymous') {
      this.authToken = this.Base64.encode(`${username}:${password}`);
      sessionStorage.setItem('authToken', this.authToken);
    }
    const location = sessionStorage.getItem('location') || 'status';
    const absUrl = window.location.href.split('login')[0];
    window.location.href = absUrl + location;
    window.location.reload();
  }

  loggedin(): void {
    const token = this.getAuthToken();
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
  }

  logout(): Observable<object> {
    sessionStorage.clear();
    return this.http.get(`${this.appService.getServerPath()}iaf/api/logout`);
  }

  getAuthToken(): string | null {
    return sessionStorage.getItem('authToken');
  }
}
