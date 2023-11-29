import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from "@angular/common/http";
import { Observable } from "rxjs";
import { AuthService } from "../services/auth.service";
import { Injectable } from "@angular/core";
import { AppService } from "../app.service";

@Injectable()
export class AuthInterceptor implements HttpInterceptor {

  constructor(
    private authService: AuthService,
    private appService: AppService
  ) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const authToken = this.authService.getAuthToken();

    if (!authToken || !req.url.startsWith(this.appService.absoluteApiPath)){
      return next.handle(req);
    }

    const authReq = req.clone({
      setHeaders: { Authorization: `Basic ${authToken}` }
    });

    return next.handle(authReq);
  }
}
