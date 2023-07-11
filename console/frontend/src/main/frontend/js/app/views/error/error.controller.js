import { appModule } from "../../app.module";

appModule.controller('ErrorPageCtrl', ['$scope', 'Api', '$state', '$interval', '$rootScope', '$timeout', 'appService', function ($scope, Api, $state, $interval, $rootScope, $timeout, appService) {
	$scope.cooldownCounter = 0;
	$scope.viewStackTrace = false;

	var cooldown = function (data) {
		$scope.cooldownCounter = 60;
		if (data.status == "error" || data.status == "INTERNAL_SERVER_ERROR") {
			appService.startupError = data.error;
			$scope.stackTrace = data.stackTrace;

			var interval = $interval(function () {
				$scope.cooldownCounter--;
				if ($scope.cooldownCounter < 1) {
					$interval.cancel(interval);
					$scope.checkState();
				}
			}, 1000);
		} else if (data.status == "SERVICE_UNAVAILABLE") {
			$state.go("pages.status");
		}
	};

	$scope.checkState = function () {
		Api.Get("server/health", function () {
			$state.go("pages.status");
			$timeout(function () { window.location.reload(); }, 50);
		}, cooldown);
	};

	$scope.checkState();
}]);
