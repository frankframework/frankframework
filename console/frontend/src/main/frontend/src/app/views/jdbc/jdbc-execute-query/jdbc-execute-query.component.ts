import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { AppConstants, AppService } from 'src/app/app.service';

interface Form {
  query: string
  queryType: string
  datasource: string
  resultType: string
  avoidLocking: boolean
  trimSpaces: boolean
}

interface Data {
  queryTypes: string[]
  resultTypes: string[]
  datasources: string[]
}

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
  form: Form = {
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
    private cookiesService: CookiesService,
    private appService: AppService
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

    var executeQueryCookie = this.cookiesService.get("executeQuery");

    this.apiService.Get("jdbc", (data: Data) => {
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

  submit(formData: Form) {
    this.processingMessage = true;

    if (!formData || !formData.query) {
      this.error = "Please specify a datasource, resulttype and query!";
      this.processingMessage = false;
      return;
    };

    if (!formData.datasource) formData.datasource = this.datasources[0] || "";
    if (!formData.resultType) formData.resultType = this.resultTypes[0] || "";

    this.cookiesService.set("executeQuery", formData);

    this.apiService.Post("jdbc/query", JSON.stringify(formData), (returnData) => {
      this.error = "";

      if (!returnData) {
        returnData = "Ok";
      };

      this.result = returnData;
      this.processingMessage = false;
    }, (errorData: { error: string }) => {
      var error = (errorData && errorData.error) ? errorData.error : "An error occured!";
      this.error = error;
      this.result = "";
      this.processingMessage = false;
    }, false);
  };

  reset() {
    this.form["query"] = "";
    this.result = "";
    this.form["datasource"] = this.datasources[0];
    this.form["resultType"] = this.resultTypes[0];
    this.form["avoidLocking"] = false;
    this.form["trimSpaces"] = false;
    this.cookiesService.remove("executeQuery");
  };
}
