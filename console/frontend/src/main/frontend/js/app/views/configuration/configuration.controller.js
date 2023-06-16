import { appModule } from "../../app.module";

appModule.controller('ShowConfigurationCtrl', ['$scope', 'Api', '$state', '$location', '$rootScope', function ($scope, Api, $state, $location, $rootScope) {
	$scope.selectedConfiguration = ($state.params.name != '') ? $state.params.name : "All";
	$scope.loadedConfiguration = ($state.params.loaded != undefined && $state.params.loaded == false);
	$rootScope.$watch('configurations', function () { $scope.configurations = $rootScope.configurations; });

	$scope.update = function () {
		getConfiguration();
	};

	var anchor = $location.hash();
	$scope.changeConfiguration = function (name) {
		$scope.selectedConfiguration = name;
		$location.hash(''); //clear the hash from the url
		anchor = null; //unset hash anchor
		getConfiguration();
	};

	$scope.updateQueryParams = function () {
		var transitionObj = {};
		if ($scope.selectedConfiguration != "All")
			transitionObj.name = $scope.selectedConfiguration;
		if (!$scope.loadedConfiguration)
			transitionObj.loaded = $scope.loadedConfiguration;

		$state.transitionTo('pages.configuration', transitionObj, { notify: false, reload: false });
	};

	$scope.clipboard = function () {
		if ($scope.configuration) {
			var el = document.createElement('textarea');
			el.value = $scope.configuration;
			el.setAttribute('readonly', '');
			el.style.position = 'absolute';
			el.style.left = '-9999px';
			document.body.appendChild(el);
			el.select();
			document.execCommand('copy');
			document.body.removeChild(el);
		}
	}

	const getConfiguration = function () {
		$scope.updateQueryParams();
		var uri = "configurations";
		if ($scope.selectedConfiguration != "All") uri += "/" + $scope.selectedConfiguration;
		if ($scope.loadedConfiguration) uri += "?loadedConfiguration=true";
		Api.Get(uri, function (data) {
			$scope.configuration = data;

			if (anchor) {
				$location.hash(anchor);
			}
		});
	};
	getConfiguration();
}]);
