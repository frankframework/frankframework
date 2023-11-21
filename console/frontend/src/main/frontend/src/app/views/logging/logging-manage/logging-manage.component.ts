import { Component, OnInit } from '@angular/core';
import { ApiService } from 'src/angularjs/app/services/api.service';
import { ToastrService } from 'src/angularjs/app/services/toastr.service';

interface Definition {
  name: string
  level: string
  appenders: string[]
}

@Component({
  selector: 'app-logging-manage',
  templateUrl: './logging-manage.component.html',
  styleUrls: ['./logging-manage.component.scss']
})
export class LoggingManageComponent implements OnInit {
  logURL: string = "server/logging";
  updateDynamicParams: boolean = false;
  loggers: Record<string, string> = {};
  errorLevels: string[] = ["DEBUG", "INFO", "WARN", "ERROR"];
  form = {
    loglevel: "DEBUG",
    logIntermediaryResults: true,
    maxMessageLength: -1,
    errorLevels: this.errorLevels,
    enableDebugger: true,
  };
  loggersLength: number = 0;
  definitions: Definition[] = [];
  alert: boolean | string = false;

  constructor(
    private apiService: ApiService,
    private toastrService: ToastrService,
  ) { };

  ngOnInit(): void {
    this.updateLogInformation();
    this.setForm();
  };

  setForm() {
    this.apiService.Get(this.logURL, (data) => {
      this.form = data;
      this.errorLevels = data.errorLevels;
    });
  };

  //Root logger level
  changeRootLoglevel(level: string) {
    this.form.loglevel = level;
  };

  //Individual level
  changeLoglevel(logger: string, level: string) {
    this.apiService.Put(this.logURL + "/settings", { logger: logger, level: level }, () => {
      this.toastrService.success("Updated logger [" + logger + "] to [" + level + "]");
      this.updateLogInformation();
    });
  };

  //Reconfigure Log4j2
  reconfigure() {
    this.apiService.Put(this.logURL + "/settings", { reconfigure: true }, () => {
      this.toastrService.success("Reconfigured log definitions!");
      this.updateLogInformation();
    });
  };

  submit(formData: any) {
    this.updateDynamicParams = true;
    this.apiService.Put(this.logURL, formData, () => {
      this.apiService.Get(this.logURL, (data) => {
        this.form = data;
        this.updateDynamicParams = false;
        this.toastrService.success("Successfully updated log configuration!");
        this.updateLogInformation();
      });
    }, () => {
      this.updateDynamicParams = false;
    });
  };

  updateLogInformation() {
    this.apiService.Get(this.logURL + "/settings", (data) => {
      this.loggers = data.loggers;
      this.loggersLength = Object.keys(data.loggers).length;
      this.definitions = data.definitions;
      console.log("DEFINITIONS:", data.definitions)

    }, function (data) {
      console.error(data);
    });
  };

  reset() {
    this.setForm();
  };
}
