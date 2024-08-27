import { HttpErrorResponse, HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { AppService } from '../app.service';
import { Router } from '@angular/router';
import { ToastService } from '../services/toast.service';

@Injectable()
export class ConnectionInterceptor implements HttpInterceptor {
  private errorCount = 0;

  constructor(
    private router: Router,
    private appService: AppService,
    private toastsService: ToastService,
  ) {}

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    return next.handle(request).pipe(
      tap({
        error: (error: HttpErrorResponse) => {
          if (error.url && !error.url.includes(this.appService.absoluteApiPath)) return;
          switch (error.status) {
            case 0: {
              if (error.url) {
                fetch(error.url, { redirect: 'manual' }).then((res) => {
                  if (res.type === 'opaqueredirect') {
                    // if the request ended in a redirect that failed, then login
                    const login_url = `${this.appService.getServerPath()}iaf/`;
                    this.router.navigate([login_url]);
                  }
                });
              }

              if (this.appService.APP_CONSTANTS['init'] == 1) {
                if (error.headers.get('Authorization') == undefined) {
                  this.toastsService.error('Failed to connect to backend!');
                } else {
                  console.warn('Authorization error');
                }
              } else if (this.appService.APP_CONSTANTS['init'] == 2 /*  && rejection.config.poller */) {
                console.warn('Connection to the server was lost!');
                this.errorCount++;
                if (this.errorCount > 2) {
                  this.toastsService.error(
                    'Server Error',
                    'Connection to the server was lost! Click to refresh the page.',
                    {
                      timeout: 0,
                      clickHandler: () => {
                        window.location.reload();
                        return true;
                      },
                    },
                  );
                }
              }
              break;
            }
            case 400: {
              this.toastsService.error(
                'Request failed',
                'Bad Request, check the application logs for more information.',
              );
              break;
            }
            case 401: {
              sessionStorage.clear();
              this.router.navigate(['login']);
              break;
            }
            case 403: {
              this.toastsService.error('Forbidden', 'You do not have the permissions to complete this operation.');
              break;
            }
            default: {
              this.toastsService.error('Server Error', error.message);
            }
          }
        },
      }),
    );
  }
}
