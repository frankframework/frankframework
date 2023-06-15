import { appModule } from "../../../app.module";

appModule.controller('CookieModalCtrl', ['$scope', 'GDPR', 'appConstants', '$rootScope', '$uibModalInstance', function ($scope, GDPR, appConstants, $rootScope, $uibModalInstance) {
	$scope.cookies = GDPR.defaults;

	$rootScope.$on('appConstants', function () {
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
