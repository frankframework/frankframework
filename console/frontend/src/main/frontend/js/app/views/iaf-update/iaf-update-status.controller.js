export default function ($scope, $location, Session) {
	$scope.release = Session.get("IAF-Release");
	if ($scope.release == undefined)
		$location.path("status");
}
