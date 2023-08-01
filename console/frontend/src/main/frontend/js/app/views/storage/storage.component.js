import { appModule } from "../../app.module";

const StorageController = function ($scope, Api, $state, SweetAlert, Misc) {
	const ctrl = this;

	ctrl.notes = [];
	ctrl.adapterName = $state.params.adapter;
	ctrl.configuration = $state.params.configuration;

	ctrl.processState = $state.params.processState;
	ctrl.storageSource = $state.params.storageSource;
	ctrl.storageSourceName = $state.params.storageSourceName;

	ctrl.$onInit = function () {
		ctrl.base_url = "configurations/" + Misc.escapeURL(ctrl.configuration) + "/adapters/" + Misc.escapeURL(ctrl.adapterName) + "/" + ctrl.storageSource + "/" + Misc.escapeURL(ctrl.storageSourceName) + "/stores/" + ctrl.processState;

		$state.current.data.pageTitle = $state.params.processState + " List";
		$state.current.data.breadcrumbs = "Adapter > " + ($state.params.storageSource == 'pipes' ? "Pipes > " + $state.params.storageSourceName + " > " : "") + $state.params.processState + " List";

		if (!ctrl.adapterName)
			return SweetAlert.Warning("Invalid URL", "No adapter name provided!");
		if (!ctrl.storageSourceName)
			return SweetAlert.Warning("Invalid URL", "No receiver or pipe name provided!");
		if (!ctrl.storageSource)
			return SweetAlert.Warning("Invalid URL", "Component type [receivers] or [pipes] is not provided in url!");
		if (!ctrl.processState)
			return SweetAlert.Warning("Invalid URL", "No storage type provided!");
	};

	ctrl.addNote = function (type, message, removeQueue) {
		ctrl.notes.push({ type: type, message: message });
	};

	ctrl.closeNote = function (index) {
		ctrl.notes.splice(index, 1);
	};

	ctrl.closeNotes = function () {
		ctrl.notes = [];
	};

	ctrl.updateTable = function () {
		var table = $('#datatable').DataTable();
		if (table)
			table.draw();
	};

	ctrl.doDeleteMessage = function (message, callback) {
		message.deleting = true;
		let messageId = message.id;
		Api.Delete(ctrl.base_url + "/messages/" + encodeURIComponent(encodeURIComponent(messageId)), function () {
			if (callback != undefined && typeof callback == 'function')
				callback(messageId);
			ctrl.addNote("success", "Successfully deleted message with ID: " + messageId);
			ctrl.updateTable();
		}, function () {
			message.deleting = false;
			ctrl.addNote("danger", "Unable to delete messages with ID: " + messageId);
			ctrl.updateTable();
		}, false);
	};

	ctrl.downloadMessage = function (messageId) {
		window.open(Misc.getServerPath() + "iaf/api/" + ctrl.base_url + "/messages/" + encodeURIComponent(encodeURIComponent(messageId)) + "/download");
	};

	ctrl.doResendMessage = function (message, callback) {
		message.resending = true;
		let messageId = message.id;
		Api.Put(ctrl.base_url + "/messages/" + encodeURIComponent(encodeURIComponent(messageId)), false, function () {
			if (callback != undefined && typeof callback == 'function')
				callback(message.id);
			ctrl.addNote("success", "Message with ID: " + messageId + " will be reprocessed");
			ctrl.updateTable();
		}, function (data) {
			message.resending = false;
			data = (data.error) ? data.error : data;
			ctrl.addNote("danger", "Unable to resend message [" + messageId + "]. " + data);
			ctrl.updateTable();
		}, false);
	};
};

appModule.component('storage', {
	controller: ['$scope', 'Api', '$state', 'SweetAlert', 'Misc', StorageController],
	template: `
        <div ui-view
			adapter-name="$ctrl.adapterName"
			base-url="$ctrl.base_url"
			storage-source-name="$ctrl.storageSourceName"
			storage-source="$ctrl.storageSource"
			process-state="$ctrl.processState",
            on-add-note="$ctrl.addNote(type, message, removeQueue)"
            on-close-note="$ctrl.closeNote(index)"
            on-close-notes="$ctrl.closeNotes()"
            on-update-table="$ctrl.updateTable()"
            on-do-delete-message="$ctrl.doDeleteMessage(message, callback)"
            on-download-message="$ctrl.downloadMessage(messageId)"
            on-do-resend-message="$ctrl.doResendMessage(message, callback)"
        ></div>
    `
});
