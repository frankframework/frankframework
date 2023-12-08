import { Component, OnInit } from '@angular/core';
import { ErrorLevels, LogInformation, LoggingService, LoggingSettings, errorLevelsConst } from '../logging.service';
import { ToastService } from 'src/app/services/toast.service';

@Component({
  selector: 'app-logging-manage',
  templateUrl: './logging-manage.component.html',
  styleUrls: ['./logging-manage.component.scss']
})
export class LoggingManageComponent implements OnInit {
  logURL: string = "server/logging";
  updateDynamicParams: boolean = false;
  loggers: Record<string, string> = {};
  errorLevels: ErrorLevels = errorLevelsConst;
  form: LoggingSettings = {
    loglevel: "DEBUG",
    logIntermediaryResults: true,
    maxMessageLength: -1,
    errorLevels: errorLevelsConst,
    enableDebugger: true,
  };
  loggersLength: number = 0;
  definitions: LogInformation['definitions'] = [];
  alert: boolean | string = false;

  constructor(
    private loggingService: LoggingService,
    private toastService: ToastService
  ) { };

  ngOnInit() {
    this.updateLogInformation();
    this.setForm();
  };

  setForm() {
    this.loggingService.getLoggingSettings().subscribe((data) => {
      this.form = data;
      this.errorLevels = data.errorLevels;
    });
  };

  //Root logger level
  changeRootLoglevel(level: typeof this.errorLevels[number]) {
    this.form.loglevel = level;
  };

  //Individual level
  changeLoglevel(logger: string, level: typeof this.errorLevels[number]) {
    this.loggingService.putLoggingSettingsChange({ logger: logger, level: level }).subscribe(() => {
      this.toastService.success("Updated logger [" + logger + "] to [" + level + "]");
      this.updateLogInformation();
    });
  };

  //Reconfigure Log4j2
  reconfigure() {
    this.loggingService.putLoggingSettingsChange({ reconfigure: true }).subscribe(() => {
      this.toastService.success("Reconfigured log definitions!");
      this.updateLogInformation();
    });
  };

  submit(formData: typeof this.form) {
    this.updateDynamicParams = true;
    this.loggingService.putLoggingSettings(formData).subscribe({ next: () => {
      this.loggingService.getLoggingSettings().subscribe((data) => {
        this.form = data;
        this.updateDynamicParams = false;
        this.toastService.success("Successfully updated log configuration!");
        this.updateLogInformation();
      });
    }, error: () => {
      this.updateDynamicParams = false;
    }});
  };

  updateLogInformation() {
    this.loggingService.getLoggingSettingsLogInformation().subscribe({ next: (data) => {
      this.loggers = data.loggers;
      this.loggersLength = Object.keys(data.loggers).length;
      this.definitions = data.definitions;
      console.log("DEFINITIONS:", data.definitions)

    }, error: (data) => {
      console.error(data);
    }});
  };

  reset() {
    this.setForm();
  };
}
