import { Component, computed, inject, Signal } from '@angular/core';
import { AppService, Configuration, ServerErrorResponse } from 'src/app/app.service';
import { JdbcService } from '../jdbc/jdbc.service';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { InputFileUploadComponent } from '../../components/input-file-upload/input-file-upload.component';
import { LaddaModule } from 'angular2-ladda';

import { QuickSubmitFormDirective } from '../../components/quick-submit-form.directive';

interface Form {
  configuration: string;
}

@Component({
  selector: 'app-liquibase',
  imports: [FormsModule, InputFileUploadComponent, LaddaModule, QuickSubmitFormDirective],
  templateUrl: './liquibase.component.html',
  styleUrls: ['./liquibase.component.scss'],
})
export class LiquibaseComponent {
  protected form: Form = {
    configuration: '',
  };
  protected file: File | null = null;
  protected generateSql: boolean = false;
  protected error: string | null = null;
  protected result: string | null = null;
  // eslint-disable-next-line unicorn/consistent-function-scoping
  protected filteredConfigurations: Signal<Configuration[]> = computed(() =>
    this.findFirstAvailabeConfiguration(this.appService.configurations()),
  );

  private readonly appService: AppService = inject(AppService);
  private readonly jdbcService: JdbcService = inject(JdbcService);

  download(): void {
    window.open(`${this.appService.getServerPath()}iaf/api/jdbc/liquibase`);
  }

  submit(formData: Form): void {
    if (!formData) formData = { configuration: '' };
    const fd = new FormData();
    this.generateSql = true;

    if (this.file) {
      fd.append('file', this.file as unknown as string);
    }

    fd.append('configuration', formData.configuration);

    this.jdbcService.postJdbcLiquibase(fd).subscribe({
      next: (returnData) => {
        this.error = null;
        this.generateSql = false;
        this.result = returnData.result;
      },
      error: (errorData: HttpErrorResponse) => {
        this.generateSql = false;
        try {
          const errorResponse = JSON.parse(errorData.error) as ServerErrorResponse | undefined;
          this.error = errorResponse ? errorResponse.error : errorData.message;
        } catch {
          this.error = errorData.message;
        }
        this.result = null;
      },
    }); // TODO no intercept
  }

  private findFirstAvailabeConfiguration(configurations: Configuration[]): Configuration[] {
    const filteredConfigurations = configurations.filter((item) => item.jdbcMigrator);

    for (const configuration of filteredConfigurations) {
      if (configuration.jdbcMigrator) {
        this.form.configuration = configuration.name;
        break;
      }
    }

    return filteredConfigurations;
  }
}
