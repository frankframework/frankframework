import { Component, Inject, OnInit } from '@angular/core';
import { AppConstants } from 'src/angularjs/app/app.module';
import { AppService } from 'src/angularjs/app/app.service';
import { ApiService } from 'src/angularjs/app/services/api.service';
import { APPCONSTANTS } from 'src/app/app.module';

@Component({
  selector: 'app-ibisstore-summary',
  templateUrl: './ibisstore-summary.component.html',
  styleUrls: ['./ibisstore-summary.component.scss']
})
export class IbisstoreSummaryComponent implements OnInit {
  datasources = [];
  form: Record<string, any> = {};
  error = "";
  result: any;

  constructor(
    private appService: AppService,
    @Inject(APPCONSTANTS) private appConstants: AppConstants,
    private apiService: ApiService,
  ) { };

  ngOnInit(): void {
    this.appService.appConstants$.subscribe(() => {
      this.form["datasource"] = this.appConstants['jdbc.datasource.default'];
    });

    this.apiService.Get("jdbc", (data) => {
      Object.assign(this, data);
      this.form["datasource"] = (this.appConstants['jdbc.datasource.default'] != undefined) ? this.appConstants['jdbc.datasource.default'] : data.datasources[0];
    });

    // TODO
    // if (this.$location.search() && this.$location.search().datasource != null) {
    //   var datasource = this.$location.search().datasource;
    //   this.fetch(datasource);
    // };
    console.warn("Location search doesn't exist anymore, needs angular new router module to recreate functionality")
  }

  fetch(datasource: []) {
    this.apiService.Post("jdbc/summary", JSON.stringify({ datasource: datasource }), (data) => {
      this.error = "";
      Object.assign(this, data);
    }, (errorData, status, errorMsg) => {
      var error = (errorData) ? errorData.error : errorMsg;
      error = error;
      this.result = "";
    }, false);
  };

  submit(formData: any) {
    if (!formData) formData = {};

    if (!formData.datasource) formData.datasource = this.datasources[0] || false;
    // TODO this.$location.search('datasource', formData.datasource);
    this.fetch(formData.datasource);
  };

  reset() {
    // TODO this.$location.search('datasource', null);
    this.result = "";
    this.error = "";
  };
}
