import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AppService, ServerErrorResponse } from 'src/app/app.service';

interface stackTrace {
  className: string;
  methodName: string;
  lineNumber: string;
}

type ServerError = ServerErrorResponse & {
  stackTrace?: stackTrace[];
};

@Component({
  selector: 'app-error',
  templateUrl: './error.component.html',
  styleUrls: ['./error.component.scss'],
})
export class ErrorComponent implements OnInit, OnDestroy {
  protected cooldownCounter: number = 0;
  protected viewStackTrace: boolean = false;
  protected stackTrace?: stackTrace[];

  private interval?: number;

  constructor(
    private router: Router,
    private appService: AppService,
  ) {}

  ngOnInit(): void {
    this.checkState();
  }

  ngOnDestroy(): void {
    if (this.interval) {
      clearInterval(this.interval);
    }
  }

  cooldown(data: ServerError): void {
    this.cooldownCounter = 60;

    if (
      data.status === 'error' ||
      data.status === 'INTERNAL_SERVER_ERROR' ||
      data.status === 'Internal Server Error' ||
      data.status === 'Gateway Timeout'
    ) {
      this.appService.updateStartupError(data.error);
      this.stackTrace = data.stackTrace;

      this.interval = window.setInterval(() => {
        this.cooldownCounter--;
        if (this.cooldownCounter < 1) {
          clearInterval(this.interval);
          this.checkState();
        }
      }, 1000);
    } else if (data.status === 'SERVICE_UNAVAILABLE' || 'Service Unavailable') {
      this.router.navigate(['/status']);
    }
  }

  checkState(): void {
    this.appService.getServerHealth().subscribe({
      next: () => {
        this.router.navigate(['/status']);
      },
      error: (response: HttpErrorResponse) => {
        try {
          const serverError: ServerError = JSON.parse(response.error);
          this.cooldown(serverError);
        } catch {
          this.cooldown({ error: response.error, status: response.statusText });
        }
      },
    });
  }
}
