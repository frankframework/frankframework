import { appModule } from "../../app.module";

appModule.controller('IBISstoreSummaryCtrl', ['$scope', 'Api', '$location', 'appConstants', function ($scope, Api, $location, appConstants) {
	$scope.datasources = {};
	$scope.form = {};

	$scope.$on('appConstants', function () {
		$scope.form.datasource = appConstants['jdbc.datasource.default'];
	});

	Api.Get("jdbc", function (data) {
		$.extend($scope, data);
		$scope.form.datasource = (appConstants['jdbc.datasource.default'] != undefined) ? appConstants['jdbc.datasource.default'] : data.datasources[0];
	});

	if ($location.search() && $location.search().datasource != null) {
		var datasource = $location.search().datasource;
		fetch(datasource);
	}
	function fetch(datasource) {
		Api.Post("jdbc/summary", JSON.stringify({ datasource: datasource }), function (data) {
			$scope.error = "";
			$.extend($scope, data);
		}, function (errorData, status, errorMsg) {
			var error = (errorData) ? errorData.error : errorMsg;
			$scope.error = error;
			$scope.result = "";
		}, false);
	}

	$scope.submit = function (formData) {
		if (!formData) formData = {};

		if (!formData.datasource) formData.datasource = $scope.datasources[0] || false;
		$location.search('datasource', formData.datasource);
		fetch(formData.datasource);
	};

	$scope.reset = function () {
		$location.search('datasource', null);
		$scope.result = "";
		$scope.error = "";
	};
}]);
