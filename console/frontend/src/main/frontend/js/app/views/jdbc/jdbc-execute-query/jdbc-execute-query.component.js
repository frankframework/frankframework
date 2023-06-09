import { appModule } from "../../../app.module";

const JdbcExecuteQueryController = function ($scope, Api, $timeout, $state, Cookies, appConstants) {
	const ctrl = this;

	ctrl.datasources = {};
	ctrl.resultTypes = {};
	ctrl.error = "";
	ctrl.processingMessage = false;
	ctrl.form = {};

	$scope.$on('appConstants', function () {
		ctrl.form.datasource = appConstants['jdbc.datasource.default'];
	});

	var executeQueryCookie = Cookies.get("executeQuery");

	Api.Get("jdbc", function (data) {
		$.extend(ctrl, data);
		ctrl.form.datasource = (appConstants['jdbc.datasource.default'] != undefined) ? appConstants['jdbc.datasource.default'] : data.datasources[0];
		ctrl.form.queryType = data.queryTypes[0];
		ctrl.form.resultType = data.resultTypes[0];
		if (executeQueryCookie) {
			ctrl.form.query = executeQueryCookie.query;
			if (data.datasources.indexOf(executeQueryCookie.datasource) !== -1) {
				ctrl.form.datasource = executeQueryCookie.datasource;
			}
			ctrl.form.resultType = executeQueryCookie.resultType;
		}

	});

	ctrl.submit = function (formData) {
		ctrl.processingMessage = true;
		if (!formData || !formData.query) {
			ctrl.error = "Please specify a datasource, resulttype and query!";
			ctrl.processingMessage = false;
			return;
		}
		if (!formData.datasource) formData.datasource = ctrl.datasources[0] || false;
		if (!formData.resultType) formData.resultType = ctrl.resultTypes[0] || false;

		Cookies.set("executeQuery", formData);

		Api.Post("jdbc/query", JSON.stringify(formData), function (returnData) {
			ctrl.error = "";
			if (returnData == undefined || returnData == "") {
				returnData = "Ok";
			}
			ctrl.result = returnData;
			ctrl.processingMessage = false;
		}, function (errorData, status, errorMsg) {
			var error = (errorData && errorData.error) ? errorData.error : "An error occured!";
			ctrl.error = error;
			ctrl.result = "";
			ctrl.processingMessage = false;
		}, false);
	};

	ctrl.reset = function () {
		ctrl.form.query = "";
		ctrl.result = "";
		ctrl.form.datasource = ctrl.datasources[0];
		ctrl.form.resultType = ctrl.resultTypes[0];
		ctrl.form.avoidLocking = false;
		ctrl.form.trimSpaces = false;
		Cookies.remove("executeQuery");
	};
};

appModule.component('jdbcExecuteQuery', {
	controller: ['$scope', 'Api', '$timeout', '$state', 'Cookies', 'appConstants', JdbcExecuteQueryController],
	templateUrl: 'js/app/views/jdbc/jdbc-execute-query/ExecuteJdbcQuery.html'
});
