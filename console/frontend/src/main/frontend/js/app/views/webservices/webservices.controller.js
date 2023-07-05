import { appModule } from "../../app.module";

appModule.controller('WebservicesCtrl', ['$scope', 'Api', 'Misc', function ($scope, Api, Misc) {
	$scope.rootURL = Misc.getServerPath();
	$scope.compileURL = function (apiListener) {
		return $scope.rootURL + "iaf/api/webservices/openapi.json?uri=" + encodeURI(apiListener.uriPattern);
	}
	Api.Get("webservices", function (data) {
		$.extend($scope, data);
	});
}]);
