import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest, HttpResponse } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable, tap } from "rxjs";

@Injectable()
export class EtagsInterceptor implements HttpInterceptor {

  private etags: Record<string, string> = {};

  constructor() {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {

    if (this.etags.hasOwnProperty(req.url)) { //If not explicitly disabled (httpOptions==false), check eTag
      const tag = this.etags[req.url];
      const matchReq = req.clone({
        setHeaders: { 'If-None-Match': tag }
      });
      this.handleResponse(next.handle(matchReq));
    }

    return this.handleResponse(next.handle(req));
  }

  handleResponse(handler: Observable<HttpEvent<any>>): Observable<HttpEvent<any>>{
    return handler.pipe(
      tap({
        next: (event) => {
          if (event instanceof HttpResponse && event.headers.has('etag')) {
            this.etags[event.url ?? ""] = event.headers.get("etag")!;
          }
        }
      })
    );
  }
}
