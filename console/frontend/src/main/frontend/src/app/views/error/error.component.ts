import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AppService } from 'src/app/app.service';

interface stackTrace {
  className: string
  methodName: string
  lineNumber: string
};

type ServerError = {
  status: string,
  error: string,
  stackTrace: any,
}

@Component({
  selector: 'app-error',
  templateUrl: './error.component.html',
  styleUrls: ['./error.component.scss']
})
export class ErrorComponent implements OnInit {
  cooldownCounter: number = 0;
  viewStackTrace: boolean = false;
  stackTrace?: stackTrace[];

  constructor(
    private router: Router,
    private http: HttpClient,
    private appService: AppService
  ) { };

  ngOnInit() {
    this.checkState();
  };

  cooldown(data: ServerError) {
    this.cooldownCounter = 60;

    if (data.status === "error" || data.status === "INTERNAL_SERVER_ERROR") {
      this.appService.updateStartupError(data.error);
      this.stackTrace = data.stackTrace;

      let interval = window.setInterval(() => {
        this.cooldownCounter--;
        if (this.cooldownCounter < 1) {
          clearInterval(interval);
          this.checkState();
        };
      }, 1000);
    } else if (data.status === "SERVICE_UNAVAILABLE") {
      this.router.navigate(['/status']);
    };
  };

  checkState() {
    this.appService.getServerHealth().subscribe({ next: () => {
      this.router.navigate(['/status']);
      /* setTimeout(() => {
        window.location.reload();
      }, 50); */
    }, error: (response: HttpErrorResponse) => this.cooldown({ error: response.error, status: response.statusText, stackTrace: [] })});
  };
}
