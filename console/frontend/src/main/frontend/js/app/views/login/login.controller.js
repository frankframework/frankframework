import { appModule } from "../../app.module";

appModule.controller('LoginCtrl', ['$scope', 'authService', '$timeout', 'Alert', function ($scope, authService, $timeout, Alert) {
	$timeout(function () {
		$scope.notifications = Alert.get();
		angular.element(".main").show();
		angular.element(".loading").hide();
		angular.element("body").addClass("gray-bg");
	}, 500);
	authService.loggedin(); //Check whether or not the client is logged in.
	$scope.credentials = {};
	$scope.login = function (credentials) {
		authService.login(credentials.username, credentials.password);
	};
}]);
