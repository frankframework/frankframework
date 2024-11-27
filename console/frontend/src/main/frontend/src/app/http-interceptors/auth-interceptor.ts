import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { Injectable } from '@angular/core';
import { AppService } from '../app.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(
    private authService: AuthService,
    private appService: AppService,
  ) {}

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    /* const authToken = this.authService.getAuthToken();

    if (
      !authToken ||
      !request.url.startsWith(this.appService.absoluteApiPath)
    ) {
      return next.handle(request);
    }

    const authRequest = request.clone({
      setHeaders: { Authorization: `Basic ${authToken}` },
    });

    return next.handle(authRequest); */
    return next.handle(request);
  }
}
