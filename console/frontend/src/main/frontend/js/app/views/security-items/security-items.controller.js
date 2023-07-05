import { appModule } from "../../app.module";

appModule.controller('SecurityItemsCtrl', ['$scope', 'Api', '$rootScope', 'appService', function ($scope, Api, $rootScope, appService) {
	$scope.sapSystems = [];
	$scope.serverProps;
	$scope.authEntries = [];
	$scope.jmsRealms = [];
	$scope.securityRoles = [];
	$scope.certificates = [];
	for (const a in appService.adapters) {
		var adapter = appService.adapters[a];
		if (adapter.pipes) {
			for (const p in adapter.pipes) {
				var pipe = adapter.pipes[p];
				if (pipe.certificate)
					$scope.certificates.push({
						adapter: a,
						pipe: p.name,
						certificate: pipe.certificate
					});
			}
		}
	}

	Api.Get("securityitems", function (data) {
		$.extend($scope, data);
	});
}]);
