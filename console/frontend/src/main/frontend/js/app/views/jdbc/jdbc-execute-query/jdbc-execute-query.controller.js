import { appModule } from "../../../app.module";

appModule.controller('ExecuteJdbcQueryCtrl', ['$scope', 'Api', '$timeout', '$state', 'Cookies', 'appConstants', function ($scope, Api, $timeout, $state, Cookies, appConstants) {
	$scope.datasources = {};
	$scope.resultTypes = {};
	$scope.error = "";
	$scope.processingMessage = false;
	$scope.form = {};

	$scope.$on('appConstants', function () {
		$scope.form.datasource = appConstants['jdbc.datasource.default'];
	});

	var executeQueryCookie = Cookies.get("executeQuery");

	Api.Get("jdbc", function (data) {
		$.extend($scope, data);
		$scope.form.datasource = (appConstants['jdbc.datasource.default'] != undefined) ? appConstants['jdbc.datasource.default'] : data.datasources[0];
		$scope.form.queryType = data.queryTypes[0];
		$scope.form.resultType = data.resultTypes[0];
		if (executeQueryCookie) {
			$scope.form.query = executeQueryCookie.query;
			if (data.datasources.indexOf(executeQueryCookie.datasource) !== -1) {
				$scope.form.datasource = executeQueryCookie.datasource;
			}
			$scope.form.resultType = executeQueryCookie.resultType;
		}

	});

	$scope.submit = function (formData) {
		$scope.processingMessage = true;
		if (!formData || !formData.query) {
			$scope.error = "Please specify a datasource, resulttype and query!";
			$scope.processingMessage = false;
			return;
		}
		if (!formData.datasource) formData.datasource = $scope.datasources[0] || false;
		if (!formData.resultType) formData.resultType = $scope.resultTypes[0] || false;

		Cookies.set("executeQuery", formData);

		Api.Post("jdbc/query", JSON.stringify(formData), function (returnData) {
			$scope.error = "";
			if (returnData == undefined || returnData == "") {
				returnData = "Ok";
			}
			$scope.result = returnData;
			$scope.processingMessage = false;
		}, function (errorData, status, errorMsg) {
			var error = (errorData && errorData.error) ? errorData.error : "An error occured!";
			$scope.error = error;
			$scope.result = "";
			$scope.processingMessage = false;
		}, false);
	};

	$scope.reset = function () {
		$scope.form.query = "";
		$scope.result = "";
		$scope.form.datasource = $scope.datasources[0];
		$scope.form.resultType = $scope.resultTypes[0];
		$scope.form.avoidLocking = false;
		$scope.form.trimSpaces = false;
		Cookies.remove("executeQuery");
	};
}]);
