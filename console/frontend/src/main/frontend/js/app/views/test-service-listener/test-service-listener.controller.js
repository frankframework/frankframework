import { appModule } from "../../app.module";

appModule.controller('TestServiceListenerCtrl', ['$scope', 'Api', 'Alert', function ($scope, Api, Alert) {
	$scope.state = [];
	$scope.file = null;
	$scope.addNote = function (type, message, removeQueue) {
		$scope.state.push({ type: type, message: message });
	};
	$scope.processingMessage = false;

	Api.Get("test-servicelistener", function (data) {
		$scope.services = data.services;
	});

	$scope.submit = function (formData) {
		$scope.result = "";
		$scope.state = [];
		if (!formData) {
			$scope.addNote("warning", "Please specify a service and message!");
			return;
		}

		var fd = new FormData();
		if (formData.service && formData.service != "")
			fd.append("service", formData.service);
		if (formData.encoding && formData.encoding != "")
			fd.append("encoding", formData.encoding);
		if (formData.message && formData.message != "") {
			var encoding = (formData.encoding && formData.encoding != "") ? ";charset=" + formData.encoding : "";
			fd.append("message", new Blob([formData.message], { type: "text/plain" + encoding }), 'message');
		}
		if ($scope.file)
			fd.append("file", $scope.file, $scope.file.name);

		if (!formData.message && !$scope.file) {
			$scope.addNote("warning", "Please specify a file or message!");
			return;
		}

		$scope.processingMessage = true;
		Api.Post("test-servicelistener", fd, function (returnData) {
			var warnLevel = "success";
			if (returnData.state == "ERROR") warnLevel = "danger";
			$scope.addNote(warnLevel, returnData.state);
			$scope.result = (returnData.result);
			$scope.processingMessage = false;
		}, function (returnData) {
			$scope.result = (returnData.result);
			$scope.processingMessage = false;
		});
	};
}]);
