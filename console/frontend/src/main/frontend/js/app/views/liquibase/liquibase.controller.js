import { appModule } from "../../app.module";

appModule.controller('LiquibaseScriptCtrl', ['$scope', 'Api', 'Misc', '$rootScope', 'appService', function ($scope, Api, Misc, $rootScope, appService) {
	$scope.form = {};
	$scope.file = null;

	let findFirstAvailabeConfiguration = function () {
		for (let i in $scope.configurations) {
			let configuration = $scope.configurations[i];
			if (configuration.jdbcMigrator) {
				$scope.form.configuration = configuration.name;
				break;
			}
		}
	}

	$scope.configurations = appService.configurations;
	$rootScope.$on('configurations', function () {
		$scope.configurations = appService.configurations;
		findFirstAvailabeConfiguration();
	});
	findFirstAvailabeConfiguration();

	$scope.download = function () {
		window.open(Misc.getServerPath() + "iaf/api/jdbc/liquibase/");
	};

	$scope.updateFile = function (file) {
		$scope.file = file;
	};

	$scope.generateSql = false;
	$scope.submit = function (formData) {
		if (!formData) formData = {};
		var fd = new FormData();
		$scope.generateSql = true;
		if ($scope.file != null) {
			fd.append("file", $scope.file);
		}

		fd.append("configuration", formData.configuration);
		Api.Post("jdbc/liquibase", fd, function (returnData) {
			$scope.error = "";
			$scope.generateSql = false;
			$.extend($scope, returnData);
		}, function (errorData, status, errorMsg) {
			$scope.generateSql = false;
			var error = (errorData) ? errorData.error : errorMsg;
			$scope.error = error;
			$scope.result = "";
		}, false);
	};

}]);
