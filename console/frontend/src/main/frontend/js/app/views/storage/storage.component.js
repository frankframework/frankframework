import { appModule } from "../../app.module";

const StorageController = function ($scope, Api, $state, SweetAlert, Misc) {
    const ctrl = this;

    ctrl.notes = [];
    ctrl.adapterName = $state.params.adapter;

    ctrl.$onInit = function () {
        ctrl.base_url = "adapters/" + Misc.escapeURL(ctrl.adapterName) + "/" + ctrl.storageSource + "/" + Misc.escapeURL(ctrl.storageSourceName) + "/stores/" + ctrl.processState;

		$state.current.data.pageTitle = $state.params.processState + " List";
		$state.current.data.breadcrumbs = "Adapter > " + ($state.params.storageSource == 'pipes' ? "Pipes > " + $state.params.storageSourceName + " > " : "") + $state.params.processState + " List";

        if (!ctrl.adapterName)
            return SweetAlert.Warning("Invalid URL", "No adapter name provided!");
        ctrl.storageSourceName = $state.params.storageSourceName;
        if (!ctrl.storageSourceName)
            return SweetAlert.Warning("Invalid URL", "No receiver or pipe name provided!");
        ctrl.storageSource = $state.params.storageSource;
        if (!ctrl.storageSource)
            return SweetAlert.Warning("Invalid URL", "Component type [receivers] or [pipes] is not provided in url!");
        ctrl.processState = $state.params.processState;
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
	template: "<div ui-view></div>"
});
