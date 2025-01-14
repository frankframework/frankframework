import { Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { AppConstants, AppService } from 'src/app/app.service';
import { InputFileUploadComponent } from 'src/app/components/input-file-upload/input-file-upload.component';
import { JdbcService } from '../../jdbc/jdbc.service';
import { ConfigurationsService } from '../configurations.service';
import { HttpErrorResponse } from '@angular/common/http';
import { Subscription } from 'rxjs';
import { FormsModule } from '@angular/forms';

import { RouterLink } from '@angular/router';
import { QuickSubmitFormDirective } from '../../../components/quick-submit-form.directive';

type Form = {
  name: string;
  datasource: string;
  encoding: string;
  version: string;
  multiple_configs: boolean;
  activate_config: boolean;
  automatic_reload: boolean;
};

@Component({
  selector: 'app-configurations-upload',
  imports: [FormsModule, InputFileUploadComponent, RouterLink, QuickSubmitFormDirective],
  templateUrl: './configurations-upload.component.html',
  styleUrls: ['./configurations-upload.component.scss'],
})
export class ConfigurationsUploadComponent implements OnInit, OnDestroy {
  @ViewChild(InputFileUploadComponent) fileInput!: InputFileUploadComponent;

  protected datasources: string[] = [];
  protected result: string = '';
  protected error: string = '';
  protected form: Form = {
    name: '',
    datasource: '',
    encoding: '',
    version: '',
    multiple_configs: false,
    activate_config: true,
    automatic_reload: false,
  };

  private file: File | null = null;
  private appConstants: AppConstants = this.appService.APP_CONSTANTS;
  private appConstantsSubscription: Subscription | null = null;

  constructor(
    private configurationsService: ConfigurationsService,
    private jdbcService: JdbcService,
    private appService: AppService,
  ) {}

  ngOnInit(): void {
    this.appConstantsSubscription = this.appService.appConstants$.subscribe(() => {
      this.form.datasource = this.appConstants['jdbc.datasource.default'] as string;
    });

    this.jdbcService.getJdbc().subscribe((data) => {
      Object.assign(this, data);
      this.form.datasource =
        this.appConstants['jdbc.datasource.default'] === undefined
          ? data.datasources[0]
          : (this.appConstants['jdbc.datasource.default'] as string);
    });
  }

  ngOnDestroy(): void {
    this.appConstantsSubscription?.unsubscribe();
  }

  updateFile(file: File | null): void {
    this.file = file;
  }

  submit(): void {
    if (this.file == null) {
      this.error = 'Please upload a file';
      return;
    }

    const fd = new FormData();
    fd.append(
      'datasource',
      this.form.datasource && this.form.datasource != '' ? this.form.datasource : this.datasources[0],
    );

    fd.append('encoding', this.form.encoding);
    fd.append('multiple_configs', this.form.multiple_configs.toString());
    fd.append('activate_config', this.form.activate_config.toString());
    fd.append('automatic_reload', this.form.automatic_reload.toString());
    fd.append('file', this.file as unknown as Blob, this.file.name);

    this.configurationsService.postConfiguration(fd).subscribe({
      next: (data) => {
        this.error = '';
        this.result = '';

        for (const pair in data) {
          if (data[pair] == 'loaded') {
            this.result += `Successfully uploaded configuration [${pair}]<br/>`;
          } else {
            this.error += `Could not upload configuration from the file [${pair}]: ${data[pair]}<br/>`;
          }
        }

        this.form = {
          name: '',
          datasource: this.datasources[0],
          encoding: '',
          version: '',
          multiple_configs: false,
          activate_config: true,
          automatic_reload: false,
        };
        if (this.file != null) {
          this.fileInput.reset();
          this.file = null;
        }
      },
      error: (errorData: HttpErrorResponse) => {
        this.error = errorData.error ? errorData.error.error : errorData.message;
        this.result = '';
      },
    }); // TODO no intercept
  }

  reset(): void {
    this.result = '';
    this.error = '';
    this.form = {
      datasource: this.datasources[0],
      name: '',
      version: '',
      encoding: '',
      multiple_configs: false,
      activate_config: true,
      automatic_reload: false,
    };
    this.fileInput.reset();
    this.file = null;
  }
}
