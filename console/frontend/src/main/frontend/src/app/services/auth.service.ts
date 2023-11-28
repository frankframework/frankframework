import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { MiscService } from './misc.service';
import { Base64Service } from './base64.service';
import { AppConstants, AppService } from '../app.service';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private authToken?: string;
  private appConstants: AppConstants;

  constructor(
    private http: HttpClient,
    private router: Router,
    private appService: AppService,
    private Base64: Base64Service
  ) {
    this.appConstants = this.appService.APP_CONSTANTS
    this.appService.appConstants$.subscribe(() => {
      this.appConstants = this.appService.APP_CONSTANTS;
    });
  }

  login(username: string, password: string): void {
    if (username != "anonymous") {
      this.authToken = this.Base64.encode(username + ':' + password);
      sessionStorage.setItem('authToken', this.authToken);
    }
    let location = sessionStorage.getItem('location') || "status";
    let absUrl = window.location.href.split("login")[0];
    window.location.href = (absUrl + location);
    window.location.reload();
  }

  loggedin(): void {
    let token = this.getAuthToken();
    if (token != null && token != "null") {
      if (this.router.url.indexOf("login") >= 0)
        this.router.navigateByUrl(sessionStorage.getItem('location') || "status");
    }
    else {
      if (this.appConstants["init"] > 0) {
        if (this.router.url.indexOf("login") < 0)
          sessionStorage.setItem('location', this.router.url || "status");
        this.router.navigateByUrl("login");
      }
    }
  }

  logout(): Observable<Object> {
    sessionStorage.clear();
    return this.http.get(this.appService.getServerPath() + "iaf/api/logout");
  }

  getAuthToken(): string | null {
    return sessionStorage.getItem('authToken');
  }
}
