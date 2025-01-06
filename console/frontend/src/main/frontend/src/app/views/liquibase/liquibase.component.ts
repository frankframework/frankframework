import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { AppService, Configuration, ServerErrorResponse } from 'src/app/app.service';
import { JdbcService } from '../jdbc/jdbc.service';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { InputFileUploadComponent } from '../../components/input-file-upload/input-file-upload.component';
import { LaddaDirective } from 'angular2-ladda';
import { NgIf } from '@angular/common';

interface Form {
  configuration: string;
}

@Component({
  selector: 'app-liquibase',
  imports: [FormsModule, InputFileUploadComponent, LaddaDirective, NgIf],
  templateUrl: './liquibase.component.html',
  styleUrls: ['./liquibase.component.scss'],
})
export class LiquibaseComponent implements OnInit, OnDestroy {
  protected form: Form = {
    configuration: '',
  };
  protected file: File | null = null;
  protected generateSql: boolean = false;
  protected error: string | null = null;
  protected result: string | null = null;
  protected filteredConfigurations: Configuration[] = [];

  private configurations: Configuration[] = [];
  private _subscriptions = new Subscription();

  constructor(
    private appService: AppService,
    private jdbcService: JdbcService,
  ) {}

  ngOnInit(): void {
    const configurationsSubscription = this.appService.configurations$.subscribe(() =>
      this.findFirstAvailabeConfiguration(),
    );
    this._subscriptions.add(configurationsSubscription);
    this.findFirstAvailabeConfiguration();
  }

  ngOnDestroy(): void {
    this._subscriptions.unsubscribe();
  }

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

  private findFirstAvailabeConfiguration(): void {
    this.configurations = this.appService.configurations;
    this.filteredConfigurations = this.configurations.filter((item) => item.jdbcMigrator);

    for (const index in this.filteredConfigurations) {
      const configuration = this.configurations[index];

      if (configuration.jdbcMigrator) {
        this.form.configuration = configuration.name;
        break;
      }
    }
  }
}
