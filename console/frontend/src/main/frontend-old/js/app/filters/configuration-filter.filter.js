import { appModule } from "../app.module";

appModule.filter('configurationFilter', function () {
	return function (adapters, $scope) {
		if (!adapters || adapters.length < 1) return [];
		var r = {};
		for (const adapterName in adapters) {
			var adapter = adapters[adapterName];

			if ((adapter.configuration == $scope.selectedConfiguration || $scope.selectedConfiguration == "All") && ($scope.filter == undefined || $scope.filter[adapter.status]))
				r[adapterName] = adapter;
		}
		return r;
	};
});
