import { appModule } from "../../app.module";

appModule.controller('TestPipelineCtrl', ['$scope', 'Api', 'Alert', '$rootScope', function ($scope, Api, Alert, $rootScope) {
	$scope.state = [];
	$scope.file = null;
	$scope.selectedConfiguration = "";

	$scope.addNote = function (type, message, removeQueue) {
		$scope.state.push({ type: type, message: message });
	};

	$rootScope.$watch('configurations', function () { $scope.configurations = $rootScope.configurations; });
	$rootScope.$watch('adapters', function () { $scope.adapters = $rootScope.adapters; });

	$scope.processingMessage = false;

	$scope.sessionKeyIndex = 1;
	$scope.sessionKeyIndices = [$scope.sessionKeyIndex];
	var sessionKeys = [];

	$scope.updateSessionKeys = function (sessionKey, index) {
		let sessionKeyIndex = sessionKeys.findIndex(f => f.index === index);	// find by index
		if (sessionKeyIndex >= 0) {
			if (sessionKey.name == "" && sessionKey.value == "") { // remove row if row is empty
				sessionKeys.splice(sessionKeyIndex, 1);
				$scope.sessionKeyIndices.splice(sessionKeyIndex, 1);
			} else { // update existing key value pair
				sessionKeys[sessionKeyIndex].key = sessionKey.name;
				sessionKeys[sessionKeyIndex].value = sessionKey.value;
			}
			$scope.state = [];
		} else if (sessionKey.name && sessionKey.name != "" && sessionKey.value && sessionKey.value != "") {
			let keyIndex = sessionKeys.findIndex(f => f.key === sessionKey.name);	// find by key
			// add new key
			if (keyIndex < 0) {
				$scope.sessionKeyIndex += 1;
				$scope.sessionKeyIndices.push($scope.sessionKeyIndex);
				sessionKeys.push({ index: index, key: sessionKey.name, value: sessionKey.value });
				$scope.state = [];
			} else { // key with the same name already exists show warning
				if ($scope.state.findIndex(f => f.message === "Session keys cannot have the same name!") < 0) //avoid adding it more than once
					$scope.addNote("warning", "Session keys cannot have the same name!");
			}
		}

	}

	$scope.updateFile = function (file) {
		$scope.file = file;
	}

	$scope.submit = function (formData) {
		$scope.result = "";
		$scope.state = [];
		if (!formData && $scope.selectedConfiguration == "") {
			$scope.addNote("warning", "Please specify a configuration");
			return;
		}

		let fd = new FormData();
		fd.append("configuration", $scope.selectedConfiguration);
		if (formData && formData.adapter && formData.adapter != "") {
			fd.append("adapter", formData.adapter);
		} else {
			$scope.addNote("warning", "Please specify an adapter!");
			return;
		}
		if (formData.encoding && formData.encoding != "")
			fd.append("encoding", formData.encoding);
		if (formData.message && formData.message != "") {
			let encoding = (formData.encoding && formData.encoding != "") ? ";charset=" + formData.encoding : "";
			fd.append("message", new Blob([formData.message], { type: "text/plain" + encoding }), 'message');
		}
		if ($scope.file)
			fd.append("file", $scope.file, $scope.file.name);

		if (sessionKeys.length > 0) {
			let incompleteKeyIndex = sessionKeys.findIndex(f => (f.key === "" || f.value === ""));
			if (incompleteKeyIndex < 0) {
				fd.append("sessionKeys", JSON.stringify(sessionKeys));
			} else {
				$scope.addNote("warning", "Please make sure all sessionkeys have name and value!");
				return;
			}
		}

		$scope.processingMessage = true;
		Api.Post("test-pipeline", fd, function (returnData) {
			var warnLevel = "success";
			if (returnData.state == "ERROR") warnLevel = "danger";
			$scope.addNote(warnLevel, returnData.state);
			$scope.result = (returnData.result);
			$scope.processingMessage = false;
			if ($scope.file != null) {
				angular.element(".form-file")[0].value = null;
				$scope.file = null;
				formData.message = returnData.message;
			}
		}, function (errorData) {
			let error = (errorData && errorData.error) ? errorData.error : "An error occured!";
			$scope.result = "";
			$scope.addNote("warning", error);
			$scope.processingMessage = false;
		});
	};
}]);
