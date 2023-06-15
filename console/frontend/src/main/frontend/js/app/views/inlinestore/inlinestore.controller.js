import { appModule } from "../../app.module";

appModule.controller('InlineStoreOverviewCtrl', ['$scope', 'Api', function ($scope, Api) {
	Api.Get("inlinestores/overview", function (data) {
		$scope.result = data;
	});

}]);
