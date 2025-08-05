import { HttpErrorResponse, HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { AppService } from '../app.service';
import { Router } from '@angular/router';
import { ToastService } from '../services/toast.service';

@Injectable()
export class ConnectionInterceptor implements HttpInterceptor {
  private readonly router: Router = inject(Router);
  private readonly appService: AppService = inject(AppService);
  private readonly toastsService: ToastService = inject(ToastService);
  private errorCount = 0;

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    return next.handle(request).pipe(
      tap({
        error: (error: HttpErrorResponse) => {
          if (error.url && !error.url.includes(this.appService.absoluteApiPath)) return;
          switch (error.status) {
            case 0: {
              this.lostConnection(error);
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

  private lostConnection(error: HttpErrorResponse): void {
    const appConstants = this.appService.appConstants();
    if (error.url) {
      fetch(error.url, { redirect: 'manual' }).then((response) => {
        if (response.type === 'opaqueredirect') {
          // if the request ended in a redirect that failed, then login
          const login_url = `${this.appService.getServerPath()}iaf/`;
          this.router.navigate([login_url]);
        }
      });
      return;
    }

    if (appConstants['init'] == 1) {
      if (error.headers.get('Authorization') == undefined) {
        this.toastsService.error('Failed to connect to backend!');
      } else {
        console.warn('Authorization error');
      }
    } else if (appConstants['init'] == 2) {
      console.warn('Connection to the server was lost!');
      this.errorCount++;
      if (this.errorCount > 2) {
        this.toastsService.error('Server Error', 'Connection to the server was lost! Click to refresh the page.', {
          timeout: 0,
          clickHandler: () => {
            window.location.reload();
            return true;
          },
        });
      }
    }
  }
}
