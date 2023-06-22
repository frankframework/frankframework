import { appModule } from "../../../app.module";

appModule.controller('BrowseJdbcTablesCtrl', ['$scope', 'Api', '$timeout', '$state', 'appConstants', function ($scope, Api, $timeout, $state, appConstants) {
	$scope.datasources = {};
	$scope.resultTypes = {};
	$scope.error = "";
	$scope.processingMessage = false;
	$scope.form = {};

	$scope.$on('appConstants', function () {
		$scope.form.datasource = appConstants['jdbc.datasource.default'];
	});

	Api.Get("jdbc", function (data) {
		$scope.datasources = data.datasources;
		$scope.form.datasource = (appConstants['jdbc.datasource.default'] != undefined) ? appConstants['jdbc.datasource.default'] : data.datasources[0];
	});
	$scope.submit = function (formData) {
		$scope.processingMessage = true;
		if (!formData || !formData.table) {
			$scope.error = "Please specify a datasource and table name!";
			$scope.processingMessage = false;
			return;
		}
		if (!formData.datasource) formData.datasource = $scope.datasources[0] || false;
		if (!formData.resultType) formData.resultType = $scope.resultTypes[0] || false;

		$scope.columnNames = [{

		}];
		var columnNameArray = [];
		$scope.result = [];

		Api.Post("jdbc/browse", JSON.stringify(formData), function (returnData) {
			$scope.error = "";
			$scope.query = returnData.query;

			var i = 0;
			for (const x in returnData.fielddefinition) {
				$scope.columnNames.push({
					id: i++,
					name: x,
					desc: returnData.fielddefinition[x]
				});
				columnNameArray.push(x);
			}

			for (const x in returnData.result) {
				var row = returnData.result[x];
				var orderedRow = [];
				for (const columnName in row) {
					var index = columnNameArray.indexOf(columnName);
					var value = row[columnName];

					if (index == -1 && columnName.indexOf("LENGTH ") > -1) {
						value += " (length)";
						index = columnNameArray.indexOf(columnName.replace("LENGTH ", ""));
					}
					orderedRow[index] = value;
				}
				$scope.result.push(orderedRow);
			}
			$scope.processingMessage = false;
		}, function (errorData) {
			var error = (errorData.error) ? errorData.error : "";
			$scope.error = error;
			$scope.query = "";
			$scope.processingMessage = false;
		}, false);
	};
	$scope.reset = function () {
		$scope.query = "";
		$scope.error = "";
	};
}]);
