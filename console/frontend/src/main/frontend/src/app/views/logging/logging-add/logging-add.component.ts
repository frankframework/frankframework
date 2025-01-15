import { Component } from '@angular/core';
import { ErrorLevels, errorLevelsConst, LoggingService } from '../logging.service';
import { ServerErrorResponse } from '../../../app.service';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { LaddaModule } from 'angular2-ladda';
import { QuickSubmitFormDirective } from '../../../components/quick-submit-form.directive';

type Form = {
  logger: string;
  level: ErrorLevels[number];
};

@Component({
  selector: 'app-logging-add',
  imports: [FormsModule, LaddaModule, QuickSubmitFormDirective, RouterLink],
  templateUrl: './logging-add.component.html',
  styleUrl: './logging-add.component.scss',
})
export class LoggingAddComponent {
  protected error: string | null = null;
  protected form: Form = {
    logger: '',
    level: 'INFO',
  };
  protected levels: ErrorLevels = errorLevelsConst;
  protected processing: boolean = false;

  constructor(
    private loggingService: LoggingService,
    private router: Router,
    private route: ActivatedRoute,
  ) {}

  submit(): void {
    const fd = new FormData();
    fd.append('logger', this.form.logger);
    fd.append('level', this.form.level);

    this.processing = true;
    this.loggingService.postLoggingSettings(fd).subscribe({
      next: () => {
        this.router.navigate(['..'], { relativeTo: this.route });
      },
      error: (errorData: HttpErrorResponse) => {
        this.processing = false;
        try {
          const errorResponse = errorData.error as ServerErrorResponse | undefined;
          this.error = errorResponse ? errorResponse.error : errorData.message;
        } catch {
          this.error = errorData.message;
        }
      },
    });
  }
}
