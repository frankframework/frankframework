import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, tap } from 'rxjs';

@Injectable()
export class AllowInterceptor implements HttpInterceptor {
  private allowed: Record<string, string> = {};

  constructor() {}

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    return next.handle(request).pipe(
      tap({
        next: (event) => {
          if (event instanceof HttpResponse && event.headers.has('allow')) {
            this.allowed[event.url ?? ''] = event.headers.get('allow')!;
          }
        },
      }),
    );
  }
}
