import { appModule } from "../../../app.module";

appModule.controller('LogSettingsCtrl', ['$scope', 'Api', 'Misc', '$timeout', '$state', 'Toastr', function ($scope, Api, Misc, $timeout, $state, Toastr) {
	$scope.updateDynamicParams = false;

	$scope.loggers = {};
	var logURL = "server/logging";
	function updateLogInformation() {
		Api.Get(logURL + "/settings", function (data) {
			$scope.loggers = data.loggers;
			$scope.loggersLength = Object.keys(data.loggers).length;
			$scope.definitions = data.definitions;
		}, function (data) {
			console.error(data);
		});
	}
	updateLogInformation();

	$scope.errorLevels = ["DEBUG", "INFO", "WARN", "ERROR"];
	Api.Get(logURL, function (data) {
		$scope.form = data;
		$scope.errorLevels = data.errorLevels;
	});

	$scope.form = {
		loglevel: "DEBUG",
		logIntermediaryResults: true,
		maxMessageLength: -1,
		errorLevels: $scope.errorLevels,
		enableDebugger: true,
	};

	//Root logger level
	$scope.changeRootLoglevel = function (level) {
		$scope.form.loglevel = level;
	};

	//Individual level
	$scope.changeLoglevel = function (logger, level) {
		Api.Put(logURL + "/settings", { logger: logger, level: level }, function () {
			Toastr.success("Updated logger [" + logger + "] to [" + level + "]");
			updateLogInformation();
		});
	};

	//Reconfigure Log4j2
	$scope.reconfigure = function () {
		Api.Put(logURL + "/settings", { reconfigure: true }, function () {
			Toastr.success("Reconfigured log definitions!");
			updateLogInformation();
		});
	}

	$scope.submit = function (formData) {
		$scope.updateDynamicParams = true;
		Api.Put(logURL, formData, function () {
			Api.Get(logURL, function (data) {
				$scope.form = data;
				$scope.updateDynamicParams = false;
				Toastr.success("Successfully updated log configuration!");
				updateLogInformation();
			});
		}, function () {
			$scope.updateDynamicParams = false;
		});
	};
}]);
