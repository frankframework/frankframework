import * as angular from "angular";
import { AppConstants, appModule } from "../../../app.module";
import { StateService } from "angular-ui-router";
import { ApiService } from "src/angularjs/app/services/api.service";
import { CookiesService } from "src/angularjs/app/services/cookies.service";
import { AppService } from "src/angularjs/app/app.service";

class JdbcExecuteQueryController {
	datasources = [];
	resultTypes = [];
	error = "";
	processingMessage = false;
	form: Record<string, any> = {};
	result: any;

	constructor(
		private $scope: angular.IScope,
		private Api: ApiService,
		private $timeout: angular.ITimeoutService,
		private $state: StateService,
		private Cookies: CookiesService,
		private appConstants: AppConstants,
		private appService: AppService
	) { };

	$onInit() {
		this.appService.appConstants$.subscribe(() => {
			this.form["datasource"] = this.appConstants['jdbc.datasource.default'];
		});

		var executeQueryCookie = this.Cookies.get("executeQuery");

		this.Api.Get("jdbc", (data) => {
			$.extend(this, data);
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

		this.Cookies.set("executeQuery", formData);

		this.Api.Post("jdbc/query", JSON.stringify(formData), (returnData) => {
			this.error = "";

			if (returnData == undefined || returnData == "") {
				returnData = "Ok";
			};

			this.result = returnData;
			this.processingMessage = false;
		}, (errorData, status, errorMsg) => {
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
		this.Cookies.remove("executeQuery");
	};
};

appModule.component('jdbcExecuteQuery', {
	controller: ['$scope', 'Api', '$timeout', '$state', 'Cookies', 'appConstants', JdbcExecuteQueryController],
	templateUrl: 'angularjs/app/views/jdbc/jdbc-execute-query/jdbc-execute-query.component.html'
});
