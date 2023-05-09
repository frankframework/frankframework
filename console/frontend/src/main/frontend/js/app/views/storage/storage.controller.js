import { appModule } from "../../app.module";

appModule.controller('StorageBaseCtrl', ['$scope', 'Api', '$state', 'SweetAlert', 'Misc', function ($scope, Api, $state, SweetAlert, Misc) {
	$scope.notes = [];
	$scope.addNote = function (type, message, removeQueue) {
		$scope.notes.push({ type: type, message: message });
	};
	$scope.closeNote = function (index) {
		$scope.notes.splice(index, 1);
	};
	$scope.closeNotes = function () {
		$scope.notes = [];
	};

	$scope.adapterName = $state.params.adapter;
	if (!$scope.adapterName)
		return SweetAlert.Warning("Invalid URL", "No adapter name provided!");
	$scope.storageSourceName = $state.params.storageSourceName;
	if (!$scope.storageSourceName)
		return SweetAlert.Warning("Invalid URL", "No receiver or pipe name provided!");
	$scope.storageSource = $state.params.storageSource;
	if (!$scope.storageSource)
		return SweetAlert.Warning("Invalid URL", "Component type [receivers] or [pipes] is not provided in url!");
	$scope.processState = $state.params.processState;
	if (!$scope.processState)
		return SweetAlert.Warning("Invalid URL", "No storage type provided!");

	$scope.base_url = "adapters/" + Misc.escapeURL($scope.adapterName) + "/" + $scope.storageSource + "/" + Misc.escapeURL($scope.storageSourceName) + "/stores/" + $scope.processState;

	$scope.updateTable = function () {
		var table = $('#datatable').DataTable();
		if (table)
			table.draw();
	};

	$scope.doDeleteMessage = function (message, callback) {
		message.deleting = true;
		let messageId = message.id;
		Api.Delete($scope.base_url + "/messages/" + encodeURIComponent(encodeURIComponent(messageId)), function () {
			if (callback != undefined && typeof callback == 'function')
				callback(messageId);
			$scope.addNote("success", "Successfully deleted message with ID: " + messageId);
			$scope.updateTable();
		}, function () {
			message.deleting = false;
			$scope.addNote("danger", "Unable to delete messages with ID: " + messageId);
			$scope.updateTable();
		}, false);
	};
	$scope.downloadMessage = function (messageId) {
		window.open(Misc.getServerPath() + "iaf/api/" + $scope.base_url + "/messages/" + encodeURIComponent(encodeURIComponent(messageId)) + "/download");
	};

	$scope.doResendMessage = function (message, callback) {
		message.resending = true;
		let messageId = message.id;
		Api.Put($scope.base_url + "/messages/" + encodeURIComponent(encodeURIComponent(messageId)), false, function () {
			if (callback != undefined && typeof callback == 'function')
				callback(message.id);
			$scope.addNote("success", "Message with ID: " + messageId + " will be reprocessed");
			$scope.updateTable();
		}, function (data) {
			message.resending = false;
			data = (data.error) ? data.error : data;
			$scope.addNote("danger", "Unable to resend message [" + messageId + "]. " + data);
			$scope.updateTable();
		}, false);
	};
}]);
