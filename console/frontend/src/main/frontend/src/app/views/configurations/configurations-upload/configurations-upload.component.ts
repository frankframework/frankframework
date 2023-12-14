import { Component, Inject, OnInit, ViewChild } from '@angular/core';
import { AppConstants, AppService } from 'src/app/app.service';
import { InputFileUploadComponent } from 'src/app/components/input-file-upload/input-file-upload.component';
import { JdbcService } from '../../jdbc/jdbc.service';
import { ConfigurationsService } from '../configurations.service';
import { HttpErrorResponse } from '@angular/common/http';

type Form = {
  name: string
  datasource: string
  encoding: string
  version: string
  multiple_configs: boolean
  activate_config: boolean
  automatic_reload: boolean
}

@Component({
  selector: 'app-configurations-upload',
  templateUrl: './configurations-upload.component.html',
  styleUrls: ['./configurations-upload.component.scss']
})
export class ConfigurationsUploadComponent implements OnInit {
  datasources: string[] = [];
  form: Form = {
    name: "",
    datasource: "",
    encoding: "",
    version: "",
    multiple_configs: false,
    activate_config: true,
    automatic_reload: false,
  };
  file: File | null = null;
  result: string = "";
  error: string = "";

  @ViewChild(InputFileUploadComponent) fileInput!: InputFileUploadComponent;

  private appConstants: AppConstants;

  constructor(
    private configurationsService: ConfigurationsService,
    private jdbcService: JdbcService,
    private appService: AppService,
  ) {
    this.appConstants = this.appService.APP_CONSTANTS;
    this.appService.appConstants$.subscribe(() => {
      this.appConstants = this.appService.APP_CONSTANTS;
      this.form.datasource = this.appConstants['jdbc.datasource.default'];
    });
  }


  ngOnInit(): void {
    this.appService.appConstants$.subscribe(() => {
      this.form.datasource = this.appConstants['jdbc.datasource.default'];
    });

    this.jdbcService.getJdbc().subscribe((data) => {
      Object.assign(this, data)
      this.form.datasource = (this.appConstants['jdbc.datasource.default'] != undefined) ? this.appConstants['jdbc.datasource.default'] : data.datasources[0];
    });
  };

  updateFile(file: File | null) {
    this.file = file;
  }

  submit() {
    if (this.file == null) return;

    var fd = new FormData();
    if (this.form.datasource && this.form.datasource != "")
      fd.append("datasource", this.form.datasource);
    else
      fd.append("datasource", this.datasources[0]);

    fd.append("encoding", this.form.encoding);
    fd.append("multiple_configs", this.form.multiple_configs.toString());
    fd.append("activate_config", this.form.activate_config.toString());
    fd.append("automatic_reload", this.form.automatic_reload.toString());
    fd.append("file", this.file as any, this.file.name);

    this.configurationsService.postConfiguration(fd).subscribe({
      next: (data) => {
        this.error = "";
        this.result = "";

        for (const pair in data) {
          if (data[pair] == "loaded") {
            this.result += "Successfully uploaded configuration [" + pair + "]<br/>";
          } else {
            this.error += "Could not upload configuration from the file [" + pair + "]: " + data[pair] + "<br/>";
          }
        }

        this.form = {
          name: "",
          datasource: this.datasources[0],
          encoding: "",
          version: "",
          multiple_configs: false,
          activate_config: true,
          automatic_reload: false,
        };
        if (this.file != null) {
          this.fileInput.reset();
          this.file = null;
        }
      }, error: (errorData: HttpErrorResponse) => {
        var error = (errorData.error) ? errorData.error : errorData.message;
        this.error = error;
        this.result = "";
      }
    }); // TODO no intercept
  };

  reset() {
    this.result = "";
    this.error = "";
    this.form = {
      datasource: this.datasources[0],
      name: "",
      version: "",
      encoding: "",
      multiple_configs: false,
      activate_config: true,
      automatic_reload: false,
    };
    this.fileInput.reset();
    this.file = null;
  };

}
