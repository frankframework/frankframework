import { appModule } from "../app.module";

appModule.filter('searchFilter', function () {
	return function (adapters, $scope) {
		if (!adapters || adapters.length < 1) return [];

		if (!$scope.searchText || $scope.searchText.length == 0) return adapters;
		var searchText = $scope.searchText.toLowerCase();

		var r = {};
		for (const adapterName in adapters) {
			var adapter = adapters[adapterName];

			if (JSON.stringify(adapter).replace(/"/g, '').toLowerCase().indexOf(searchText) > -1)
				r[adapterName] = adapter;
		}
		return r;
	};
});
