export default function ($scope, authService, $location, $state) {
	authService.loggedin(); //Check if the user is logged in.
	$scope.monitoring = false;
	$scope.config_database = false;

	angular.element(".main").show();
	angular.element(".loading").remove();
}
