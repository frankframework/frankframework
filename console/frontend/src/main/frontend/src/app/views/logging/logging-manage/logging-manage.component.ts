import { Component, OnInit } from '@angular/core';
import { ErrorLevels, errorLevelsConst, LoggingService, LoggingSettings, LogInformation } from '../logging.service';
import { ToastService } from 'src/app/services/toast.service';
import { ActivatedRoute, Router } from '@angular/router';

@Component({
  selector: 'app-logging-manage',
  templateUrl: './logging-manage.component.html',
  styleUrls: ['./logging-manage.component.scss'],
})
export class LoggingManageComponent implements OnInit {
  protected updateDynamicParams: boolean = false;
  protected loggers: Record<string, string> = {};
  protected errorLevels: ErrorLevels = errorLevelsConst;
  protected form: LoggingSettings = {
    loglevel: 'DEBUG',
    logIntermediaryResults: true,
    maxMessageLength: -1,
    errorLevels: errorLevelsConst,
    enableDebugger: true,
  };
  protected loggersLength: number = 0;
  protected definitions: LogInformation['definitions'] = [];
  protected alert: string | null = null;

  constructor(
    private loggingService: LoggingService,
    private toastService: ToastService,
    private router: Router,
    private route: ActivatedRoute,
  ) {}

  ngOnInit(): void {
    this.updateLogInformation();
    this.setForm();
  }

  setForm(): void {
    this.loggingService.getLoggingSettings().subscribe((data) => {
      this.form = data;
      this.errorLevels = data.errorLevels;
    });
  }

  //Root logger level
  changeRootLoglevel(level: (typeof this.errorLevels)[number]): void {
    this.form.loglevel = level;
  }

  //Individual level
  changeLoglevel(logger: string | unknown, level: (typeof this.errorLevels)[number]): void {
    this.loggingService.putLoggingSettingsChange({ logger: logger as string, level: level }).subscribe(() => {
      this.toastService.success(`Updated logger [${logger}] to [${level}]`);
      this.updateLogInformation();
    });
  }

  addLogger(): void {
    this.router.navigate(['add'], { relativeTo: this.route });
  }

  //Reconfigure Log4j2
  reconfigure(): void {
    this.loggingService.putLoggingSettingsChange({ reconfigure: true }).subscribe(() => {
      this.toastService.success('Reconfigured log definitions!');
      this.updateLogInformation();
    });
  }

  submit(formData: typeof this.form): void {
    this.updateDynamicParams = true;
    this.loggingService.putLoggingSettings(formData).subscribe({
      next: () => {
        this.loggingService.getLoggingSettings().subscribe((data) => {
          this.form = data;
          this.updateDynamicParams = false;
          this.toastService.success('Successfully updated log configuration!');
          this.updateLogInformation();
        });
      },
      error: () => {
        this.updateDynamicParams = false;
      },
    });
  }

  updateLogInformation(): void {
    this.loggingService.getLoggingSettingsLogInformation().subscribe({
      next: (data) => {
        this.loggers = data.loggers;
        this.loggersLength = Object.keys(data.loggers).length;
        this.definitions = data.definitions;
      },
      error: (data) => {
        console.error(data);
      },
    });
  }

  reset(): void {
    this.setForm();
  }
}
