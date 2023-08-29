import { appModule } from "../../../app.module";

appModule.controller('CookieModalCtrl', ['$scope', 'GDPR', 'appConstants', 'appService', '$uibModalInstance', function ($scope, GDPR, appConstants, appService, $uibModalInstance) {
	$scope.cookies = GDPR.defaults;

  appService.appConstants$.subscribe(function () {
		$scope.cookies = {
			necessary: true,
			personalization: appConstants.getBoolean("console.cookies.personalization", true),
			functional: appConstants.getBoolean("console.cookies.functional", true)
		};
	});

	$scope.consentAllCookies = function () {
		$scope.savePreferences({
			necessary: true,
			personalization: true,
			functional: true
		});
	};

	$scope.close = function () {
		$uibModalInstance.close();
	}

	$scope.savePreferences = function (cookies) {
		GDPR.setSettings(cookies);
		$uibModalInstance.close();
	};
}]);
