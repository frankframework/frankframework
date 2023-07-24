import { appModule } from "../../app.module";

const ErrorController = function ($scope, Api, $state, $interval, $rootScope, $timeout, appService) {
    const ctrl = this;

    ctrl.cooldownCounter = 0;
	ctrl.viewStackTrace = false;

    ctrl.$onInit = function () {
		ctrl.checkState();
	};

	var cooldown = function (data) {
		ctrl.cooldownCounter = 60;

		if (data.status == "error" || data.status == "INTERNAL_SERVER_ERROR") {
			appService.startupError = data.error;
			ctrl.stackTrace = data.stackTrace;

			var interval = $interval(function () {
				ctrl.cooldownCounter--;
				if ($scope.cooldownCounter < 1) {
					$interval.cancel(interval);
					ctrl.checkState();
				}
			}, 1000);
		} else if (data.status == "SERVICE_UNAVAILABLE") {
			$state.go("pages.status");
		}
	};

	ctrl.checkState = function () {
		Api.Get("server/health", function () {
			$state.go("pages.status");
			$timeout(function () { window.location.reload(); }, 50);
		}, cooldown);
	};
};

appModule.component('error', {
    controller: ['$scope', 'Api', '$state', '$interval', '$rootScope', '$timeout', 'appService', ErrorController],
    templateUrl: 'js/app/views/error/error.component.html'
});
