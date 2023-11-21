import { Component, OnInit } from '@angular/core';
import { ApiService } from 'src/angularjs/app/services/api.service';
import { StateService } from "@uirouter/angularjs";
import { AppService } from 'src/app/app.service';
import { ServerError } from 'src/angularjs/app/views/error/error.component';

interface stackTrace {
  className: string
  methodName: string
  lineNumber: string
};

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
    private apiService: ApiService,
    private stateService: StateService,
    private appService: AppService
  ) { };

  ngOnInit(): void {
    this.checkState();
  };

  cooldown(data: ServerError) {
    this.cooldownCounter = 60;

    if (data.status == "error" || data.status == "INTERNAL_SERVER_ERROR") {
      this.appService.updateStartupError(data.error);
      this.stackTrace = data.stackTrace;

      var interval = setInterval(() => {
        this.cooldownCounter--;
        if (this.cooldownCounter < 1) {
          clearInterval(interval);
          this.checkState();
        };
      }, 1000);
    } else if (data.status == "SERVICE_UNAVAILABLE") {
      this.stateService.go("pages.status");
    };
  };

  checkState() {
    this.apiService.Get("server/health", () => {
      this.stateService.go("pages.status");
      setTimeout(() => {
        window.location.reload();
      }, 50);
    }, (data, status, statusText) => this.cooldown({ error: data, status: statusText, stackTrace: [] }));
  };
}
