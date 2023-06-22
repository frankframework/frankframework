import { appModule } from "../../app.module";

appModule.controller('LoadingPageCtrl', ['$scope', 'Api', '$state', function ($scope, Api, $state) {
	Api.Get("server/health", function () {
		$state.go("pages.status");
	}, function (data, statusCode) {
		if (statusCode == 401) return;

		if (data.status == "SERVICE_UNAVAILABLE") {
			$state.go("pages.status");
		} else {
			$state.go("pages.errorpage");
		}
	});
}]);
