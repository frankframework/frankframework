import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest, HttpResponse } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable, tap } from "rxjs";

@Injectable()
export class AllowInterceptor implements HttpInterceptor {

  private allowed: Record<string, string> = {};

  constructor() { }

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return next.handle(req).pipe(
      tap({
        next: (event) => {
          if (event instanceof HttpResponse && event.headers.has('allow')) {
            this.allowed[event.url ?? ""] = event.headers.get("allow")!;
          }
        }
      })
    );
  }
}
