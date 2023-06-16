import { appModule } from "../../../app.module";

appModule.controller('UploadConfigurationsCtrl', ['$scope', 'Api', 'appConstants', function ($scope, Api, appConstants) {
	$scope.datasources = {};
	$scope.form = {};

	$scope.$on('appConstants', function () {
		$scope.form.datasource = appConstants['jdbc.datasource.default'];
	});

	Api.Get("jdbc", function (data) {
		$.extend($scope, data);
		$scope.form.datasource = (appConstants['jdbc.datasource.default'] != undefined) ? appConstants['jdbc.datasource.default'] : data.datasources[0];
	});

	$scope.form = {
		datasource: "",
		encoding: "",
		multiple_configs: false,
		activate_config: true,
		automatic_reload: false,
	};

	$scope.file = null;

	$scope.updateFile = function (file) {
		$scope.file = file;
	};

	$scope.submit = function () {
		if ($scope.file == null) return;

		var fd = new FormData();
		if ($scope.form.datasource && $scope.form.datasource != "")
			fd.append("datasource", $scope.form.datasource);
		else
			fd.append("datasource", $scope.datasources[0]);

		fd.append("encoding", $scope.form.encoding);
		fd.append("multiple_configs", $scope.form.multiple_configs);
		fd.append("activate_config", $scope.form.activate_config);
		fd.append("automatic_reload", $scope.form.automatic_reload);
		fd.append("file", $scope.file, $scope.file.name);

		Api.Post("configurations", fd, function (data) {
			$scope.error = "";
			$scope.result = "";
			for (const pair in data) {
				if (data[pair] == "loaded") {
					$scope.result += "Successfully uploaded configuration [" + pair + "]<br/>";
				} else {
					$scope.error += "Could not upload configuration from the file [" + pair + "]: " + data[pair] + "<br/>";
				}
			}

			$scope.form = {
				datasource: $scope.datasources[0],
				encoding: "",
				multiple_configs: false,
				activate_config: true,
				automatic_reload: false,
			};
			if ($scope.file != null) {
				angular.element(".form-file")[0].value = null;
				$scope.file = null;
			}
		}, function (errorData, status, errorMsg) {
			var error = (errorData) ? errorData.error : errorMsg;
			$scope.error = error;
			$scope.result = "";
		}, false);
	};

	$scope.reset = function () {
		$scope.result = "";
		$scope.error = "";
		$scope.form = {
			datasource: $scope.datasources[0],
			name: "",
			version: "",
			encoding: "",
			multiple_configs: false,
			activate_config: true,
			automatic_reload: false,
		};
	};
}]);
