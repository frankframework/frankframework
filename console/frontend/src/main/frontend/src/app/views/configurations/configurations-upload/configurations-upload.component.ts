import { Component, Inject, OnInit } from '@angular/core';
import { AppConstants } from 'src/angularjs/app/app.module';
import { ApiService } from 'src/angularjs/app/services/api.service';
import { AppService } from 'src/angularjs/app/app.service';
import { APPCONSTANTS } from 'src/app/app.module';
import { File } from 'src/angularjs/app/app.service';

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
  file: File | null = {
    name: ""
  };
  result: string = "";
  error: string = "";

  constructor(
    private Api: ApiService,
    @Inject(APPCONSTANTS) private appConstants: AppConstants,
    private appService: AppService,
  ) { };


  ngOnInit(): void {
    this.appService.appConstants$.subscribe(() => {
      this.form.datasource = this.appConstants['jdbc.datasource.default'];
    });

    this.Api.Get("jdbc", (data) => {
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

    this.Api.Post("configurations", fd, (data) => {
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
        // Todo: angular.element(".form-file")[0].value = null;
        this.file = null;
      }
    }, (errorData, status, errorMsg) => {
      var error = (errorData) ? errorData.error : errorMsg;
      this.error = error;
      this.result = "";
    }, false);
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
  };

}
