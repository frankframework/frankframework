import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, tap } from 'rxjs';

@Injectable()
export class EtagsInterceptor implements HttpInterceptor {
  private etags: Record<string, string> = {};

  constructor() {}

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    if (this.etags.hasOwnProperty(request.url)) {
      //If not explicitly disabled (httpOptions==false), check eTag
      const tag = this.etags[request.url];
      const matchRequest = request.clone({
        setHeaders: { 'If-None-Match': tag },
      });
      this.handleResponse(next.handle(matchRequest));
    }

    return this.handleResponse(next.handle(request));
  }

  handleResponse(handler: Observable<HttpEvent<unknown>>): Observable<HttpEvent<unknown>> {
    return handler.pipe(
      tap({
        next: (event) => {
          if (event instanceof HttpResponse && event.headers.has('etag')) {
            this.etags[event.url ?? ''] = event.headers.get('etag')!;
          }
        },
      }),
    );
  }
}
