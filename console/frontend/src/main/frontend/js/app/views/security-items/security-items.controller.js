import { appModule } from "../../app.module";

appModule.controller('SecurityItemsCtrl', ['$scope', 'Api', '$rootScope', function ($scope, Api, $rootScope) {
	$scope.sapSystems = [];
	$scope.serverProps;
	$scope.authEntries = [];
	$scope.jmsRealms = [];
	$scope.securityRoles = [];
	$scope.certificates = [];
	for (const a in $rootScope.adapters) {
		var adapter = $rootScope.adapters[a];
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
