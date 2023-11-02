import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { AppConstants, AppService } from 'src/app/app.service';
import { WebStorageService } from 'src/app/services/web-storage.service';
import { JdbcQueryForm, JdbcService } from '../jdbc.service';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-jdbc-execute-query',
  templateUrl: './jdbc-execute-query.component.html',
  styleUrls: ['./jdbc-execute-query.component.scss']
})
export class JdbcExecuteQueryComponent implements OnInit, OnDestroy {
  datasources: string[] = [];
  resultTypes: string[] = [];
  queryTypes: string[] = [];
  error: string = "";
  processingMessage: boolean = false;
  form: JdbcQueryForm = {
    query: "",
    queryType: "",
    datasource: "",
    resultType: "",
    avoidLocking: false,
    trimSpaces: false
  };;
  result: string = "";

  private _subscriptions = new Subscription();
  private appConstants: AppConstants;

  constructor(
    private webStorageService: WebStorageService,
    private appService: AppService,
    private jdbcService: JdbcService
  ) {
    this.appConstants = this.appService.APP_CONSTANTS;
    const appConstantsSubscription = this.appService.appConstants$.subscribe(() => {
      this.appConstants = this.appService.APP_CONSTANTS;
    });
    this._subscriptions.add(appConstantsSubscription)
  }

  ngOnInit(): void {
    const appConstantsSubscription = this.appService.appConstants$.subscribe(() => {
      this.form["datasource"] = this.appConstants['jdbc.datasource.default'];
    });
    this._subscriptions.add(appConstantsSubscription);

    var executeQueryCookie = this.webStorageService.get("executeQuery");

    this.jdbcService.getJdbc().subscribe((data) => {
      Object.assign(this, data);

      this.form["datasource"] = (this.appConstants['jdbc.datasource.default'] != undefined) ? this.appConstants['jdbc.datasource.default'] : data.datasources[0];
      this.form["queryType"] = data.queryTypes[0];
      this.form["resultType"] = data.resultTypes[0];

      if (executeQueryCookie) {
        this.form["query"] = executeQueryCookie.query;

        if (data.datasources.indexOf(executeQueryCookie.datasource) !== -1) {
          this.form["datasource"] = executeQueryCookie.datasource;
        };

        this.form["resultType"] = executeQueryCookie.resultType;
      };
    });
  }

  ngOnDestroy() {
    this._subscriptions.unsubscribe();
  }

  submit(formData: JdbcQueryForm) {
    this.processingMessage = true;

    if (!formData || !formData.query) {
      this.error = "Please specify a datasource, resulttype and query!";
      this.processingMessage = false;
      return;
    };

    if (!formData.datasource) formData.datasource = this.datasources[0] || "";
    if (!formData.resultType) formData.resultType = this.resultTypes[0] || "";

    this.webStorageService.set("executeQuery", formData);

    this.jdbcService.postJdbcQuery(formData).subscribe({
      next: returnData => {
        this.error = "";

        if (!returnData) {
          returnData = "Ok";
        };

        this.result = returnData;
        this.processingMessage = false;
      }, error: (errorData: HttpErrorResponse) => {
        const error = (errorData && errorData.error) ? errorData.error : "An error occured!";
        this.error = typeof error === 'object' ? error.error : error;
        this.result = "";
        this.processingMessage = false;
      }
    }); // TODO no intercept
  };

  reset() {
    this.form["query"] = "";
    this.result = "";
    this.form["datasource"] = this.datasources[0];
    this.form["resultType"] = this.resultTypes[0];
    this.form["avoidLocking"] = false;
    this.form["trimSpaces"] = false;
    this.webStorageService.remove("executeQuery");
  };
}
