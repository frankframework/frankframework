import { appModule } from "../../../app.module";

appModule.controller('AdapterViewStorageIdCtrl', ['$scope', 'Api', '$state', 'SweetAlert', function ($scope, Api, $state, SweetAlert) {
	$scope.message = {};
	$scope.closeNotes();

	$scope.message.id = $state.params.messageId;
	if (!$scope.message.id)
		return SweetAlert.Warning("Invalid URL", "No message id provided!");

	Api.Get($scope.base_url + "/messages/" + encodeURIComponent(encodeURIComponent($scope.message.id)), function (data) {
		$scope.metadata = data;
	}, function (errorData, statusCode, errorMsg) {
		let error = (errorData) ? errorData.error : errorMsg;
		if (statusCode == 500) {
			SweetAlert.Warning("An error occured while opening the message", "message id [" + $scope.message.id + "] error [" + error + "]");
		} else {
			SweetAlert.Warning("Message not found", "message id [" + $scope.message.id + "] error [" + error + "]");
		}
		$state.go("pages.storage.list", { adapter: $scope.adapterName, storageSource: $scope.storageSource, storageSourceName: $scope.storageSourceName, processState: $scope.processState });
	});

	$scope.resendMessage = function (message) {
		$scope.doResendMessage(message, function (messageId) {
			//Go back to the storage list if successful
			$state.go("pages.storage.list", { adapter: $scope.adapterName, storageSource: $scope.storageSource, storageSourceName: $scope.storageSourceName, processState: $scope.processState });
		});
	};

	$scope.deleteMessage = function (message) {
		$scope.doDeleteMessage(message, function (messageId) {
			//Go back to the storage list if successful
			$state.go("pages.storage.list", { adapter: $scope.adapterName, storageSource: $scope.storageSource, storageSourceName: $scope.storageSourceName, processState: $scope.processState });
		});
	};
}]);
