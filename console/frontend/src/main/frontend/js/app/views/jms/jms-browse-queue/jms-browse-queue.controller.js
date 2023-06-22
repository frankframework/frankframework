import { appModule } from "../../../app.module";

appModule.controller('BrowseJmsQueueCtrl', ['$scope', 'Api', 'Cookies', function ($scope, Api, Cookies) {
	$scope.destinationTypes = ["QUEUE", "TOPIC"];
	$scope.form = {};
	Api.Get("jms", function (data) {
		$.extend($scope, data);
		angular.element("select[name='type']").val($scope.destinationTypes[0]);
	});

	var browseJmsQueue = Cookies.get("browseJmsQueue");
	if (browseJmsQueue) {
		$scope.form = browseJmsQueue;
	}

	$scope.messages = [];
	$scope.numberOfMessages = -1;
	$scope.processing = false;
	$scope.submit = function (formData) {
		$scope.processing = true;
		if (!formData || !formData.destination) {
			$scope.error = "Please specify a connection factory and destination!";
			return;
		}

		Cookies.set("browseJmsQueue", formData);
		if (!formData.connectionFactory) formData.connectionFactory = $scope.connectionFactories[0] || false;
		if (!formData.type) formData.type = $scope.destinationTypes[0] || false;

		Api.Post("jms/browse", JSON.stringify(formData), function (data) {
			$.extend($scope, data);
			if (!data.messages) {
				$scope.messages = [];
			}
			$scope.error = "";
			$scope.processing = false;
		}, function (errorData, status, errorMsg) {
			$scope.error = (errorData && errorData.error) ? errorData.error : errorMsg;
			$scope.processing = false;
		});
	};

	$scope.reset = function () {
		$scope.error = "";
		if (!$scope.form) return;
		if ($scope.form.destination)
			$scope.form.destination = "";
		if ($scope.form.rowNumbersOnly)
			$scope.form.rowNumbersOnly = "";
		if ($scope.form.type)
			$scope.form.type = $scope.destinationTypes[0];

		$scope.messages = [];
		$scope.numberOfMessages = -1;
		$scope.processing = false;
	};
}]);
