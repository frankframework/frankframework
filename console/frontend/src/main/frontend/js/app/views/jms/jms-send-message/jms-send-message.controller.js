import { appModule } from "../../../app.module";

appModule.controller('SendJmsMessageCtrl', ['$scope', 'Api', function ($scope, Api) {
	$scope.destinationTypes = ["QUEUE", "TOPIC"];
	$scope.processing = false;
	Api.Get("jms", function (data) {
		$.extend($scope, data);
		angular.element("select[name='type']").val($scope.destinationTypes[0]);
	});

	$scope.file = null;

	$scope.updateFile = function (file) {
		$scope.file = file;
	};

	$scope.submit = function (formData) {
		$scope.processing = true;
		if (!formData) return;

		var fd = new FormData();
		if (formData.connectionFactory && formData.connectionFactory != "")
			fd.append("connectionFactory", formData.connectionFactory);
		else
			fd.append("connectionFactory", $scope.connectionFactories[0]);
		if (formData.destination && formData.destination != "")
			fd.append("destination", formData.destination);
		if (formData.type && formData.type != "")
			fd.append("type", formData.type);
		else
			fd.append("type", $scope.destinationTypes[0]);
		if (formData.replyTo && formData.replyTo != "")
			fd.append("replyTo", formData.replyTo);
		if (formData.persistent && formData.persistent != "")
			fd.append("persistent", formData.persistent);
		if (formData.synchronous && formData.synchronous != "")
			fd.append("synchronous", formData.synchronous);
		if (formData.lookupDestination && formData.lookupDestination != "")
			fd.append("lookupDestination", formData.lookupDestination);

		if (formData.propertyKey && formData.propertyKey != "" && formData.propertyValue && formData.propertyValue != "")
			fd.append("property", formData.propertyKey + "," + formData.propertyValue);
		if (formData.message && formData.message != "") {
			var encoding = (formData.encoding && formData.encoding != "") ? ";charset=" + formData.encoding : "";
			fd.append("message", new Blob([formData.message], { type: "text/plain" + encoding }), 'message');
		}
		if ($scope.file)
			fd.append("file", $scope.file, $scope.file.name);
		if (formData.encoding && formData.encoding != "")
			fd.append("encoding", formData.encoding);

		if (!formData.message && !$scope.file) {
			$scope.error = "Please specify a file or message!";
			$scope.processing = false;
			return;
		}

		Api.Post("jms/message", fd, function (returnData) {
			$scope.error = null;
			$scope.processing = false;
		}, function (errorData, status, errorMsg) {
			$scope.processing = false;
			errorMsg = (errorMsg) ? errorMsg : "An unknown error occured, check the logs for more info.";
			$scope.error = (errorData.error) ? errorData.error : errorMsg;
		});
	};

	$scope.reset = function () {
		$scope.error = "";
		if (!$scope.form) return;
		if ($scope.form.destination)
			$scope.form.destination = "";
		if ($scope.form.replyTo)
			$scope.form.replyTo = "";
		if ($scope.form.message)
			$scope.form.message = "";
		if ($scope.form.persistent)
			$scope.form.persistent = "";
		if ($scope.form.type)
			$scope.form.type = $scope.destinationTypes[0];
	};
}]);
