import { Component, Inject, OnInit } from '@angular/core';
import { StateService } from 'angular-ui-router';
import { AppConstants } from 'src/angularjs/app/app.module';
import { ApiService, CookiesService } from 'src/app/services.types';

@Component({
  selector: 'app-jdbc-execute-query',
  templateUrl: './jdbc-execute-query.component.html',
  styleUrls: ['./jdbc-execute-query.component.scss']
})
export class JdbcExecuteQueryComponent implements OnInit {
  datasources = [];
  resultTypes = [];
  queryTypes = [];
  error = "";
  processingMessage = false;
  form: Record<string, any> = {};
  result: any;

  constructor(
    @Inject("$scope") private $scope: angular.IScope,
    @Inject("apiService") private apiService: ApiService,
    @Inject("$timeout") private $timeout: angular.ITimeoutService,
    @Inject("$state") private $state: StateService,
    @Inject("cookiesService") private cookiesService: CookiesService,
    @Inject("appConstants") private appConstants: AppConstants
  ) { };

  ngOnInit(): void {
    this.$scope.$on('appConstants', () => {
      this.form["datasource"] = this.appConstants['jdbc.datasource.default'];
    });

    var executeQueryCookie = this.cookiesService.get("executeQuery");

    this.apiService.Get("jdbc", (data) => {
      this.updateData(data);
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
  };

  submit(formData: any) {
    this.processingMessage = true;

    if (!formData || !formData.query) {
      this.error = "Please specify a datasource, resulttype and query!";
      this.processingMessage = false;
      return;
    };

    if (!formData.datasource) formData.datasource = this.datasources[0] || false;
    if (!formData.resultType) formData.resultType = this.resultTypes[0] || false;

    this.cookiesService.set("executeQuery", formData);

    this.apiService.Post("jdbc/query", JSON.stringify(formData), (returnData) => {
      this.error = "";

      if (!returnData) {
        returnData = "Ok";
      };

      this.result = returnData;
      this.processingMessage = false;
    }, (errorData) => {
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

  updateData(data: any) {
    for (const key in data) {
      if (data.hasOwnProperty(key)) {
        this[key] = data[key];
      };
    };
  };
}
