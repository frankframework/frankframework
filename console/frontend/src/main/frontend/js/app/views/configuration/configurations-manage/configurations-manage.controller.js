import { appModule } from "../../../app.module";

//** Ctrls **//
appModule.controller('ManageConfigurationsCtrl', ['$scope', 'Api', '$rootScope', function ($scope, Api, $rootScope) {
	$rootScope.$watch('configurations', function () { $scope.configurations = $rootScope.configurations; });
	Api.Get("server/configurations", function (data) {
		$scope.updateConfigurations(data);
	});
}]);
