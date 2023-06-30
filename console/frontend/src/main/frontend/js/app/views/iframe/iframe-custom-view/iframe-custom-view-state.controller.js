export default function ($scope, Misc, $state, $window) {
	if ($state.params.url == "")
		$state.go('pages.status');

	if ($state.params.url.indexOf("http") > -1) {
		$window.open($state.params.url, $state.params.name);
		$scope.redirectURL = $state.params.url;
	}
	else
		$scope.url = Misc.getServerPath() + $state.params.url;
}
