import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AppService } from 'src/app/app.service';

interface stackTrace {
  className: string;
  methodName: string;
  lineNumber: string;
}

type ServerError = {
  status: string;
  error: string;
  stackTrace?: stackTrace[];
};

@Component({
  selector: 'app-error',
  imports: [RouterLink],
  templateUrl: './error.component.html',
  styleUrls: ['./error.component.scss'],
})
export class ErrorComponent implements OnInit, OnDestroy {
  protected cooldownCounter: number = 0;
  protected viewStackTrace: boolean = false;
  protected stackTrace?: stackTrace[];

  private interval?: number;
  private readonly router: Router = inject(Router);
  private readonly appService: AppService = inject(AppService);

  ngOnInit(): void {
    this.checkState();
  }

  ngOnDestroy(): void {
    if (this.interval) {
      clearInterval(this.interval);
    }
  }

  cooldown(httpCode: number, error: string, stackTrace?: stackTrace[]): void {
    if (httpCode < 400 || httpCode > 502) this.router.navigate(['/status']);

    this.cooldownCounter = 60;
    this.appService.startupError.set(error);
    this.stackTrace = stackTrace;

    this.interval = window.setInterval(() => {
      this.cooldownCounter--;
      if (this.cooldownCounter < 1) {
        clearInterval(this.interval);
        this.checkState();
      }
    }, 1000);
  }

  checkState(): void {
    this.appService.getServerHealth().subscribe({
      next: () => this.router.navigate(['/status']),
      error: (response: HttpErrorResponse) => {
        try {
          const errorResponse: ServerError = JSON.parse(response.error);
          this.cooldown(response.status, errorResponse.error, errorResponse.stackTrace);
        } catch {
          this.cooldown(response.status, response.error);
        }
      },
    });
  }
}
