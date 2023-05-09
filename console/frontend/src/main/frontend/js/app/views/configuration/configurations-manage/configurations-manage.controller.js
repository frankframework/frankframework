import { appModule } from "../../../app.module";

//** Ctrls **//
appModule.controller('ManageConfigurationsCtrl', ['$scope', 'Api', function ($scope, Api) {
	Api.Get("server/configurations", function (data) {
		$scope.updateConfigurations(data);
	});
}]);
