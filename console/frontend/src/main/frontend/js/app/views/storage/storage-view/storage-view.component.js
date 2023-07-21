import { appModule } from "../../../app.module";

const StorageViewController = function ($scope, Api, $state, SweetAlert) {
    const ctrl = this;

    ctrl.message = {};
    ctrl.message.id = $state.params.messageId;

    ctrl.$onInit = function () {
        ctrl.closeNotes();

        if (!ctrl.message.id)
            return SweetAlert.Warning("Invalid URL", "No message id provided!");

        Api.Get(ctrl.base_url + "/messages/" + encodeURIComponent(encodeURIComponent(ctrl.message.id)), function (data) {
            ctrl.metadata = data;
        }, function (errorData, statusCode, errorMsg) {
            let error = (errorData) ? errorData.error : errorMsg;
            if (statusCode == 500) {
                SweetAlert.Warning("An error occured while opening the message", "message id [" + ctrl.message.id + "] error [" + error + "]");
            } else {
                SweetAlert.Warning("Message not found", "message id [" + ctrl.message.id + "] error [" + error + "]");
            }
            $state.go("pages.storage.list", { adapter: ctrl.adapterName, storageSource: ctrl.storageSource, storageSourceName: ctrl.storageSourceName, processState: ctrl.processState });
        });
    };

    ctrl.resendMessage = function (message) {
        ctrl.doResendMessage(message, function (messageId) {
            //Go back to the storage list if successful
            ctrl.go("pages.storage.list", { adapter: ctrl.adapterName, storageSource: ctrl.storageSource, storageSourceName: ctrl.storageSourceName, processState: ctrl.processState });
        });
    };

    ctrl.deleteMessage = function (message) {
        ctrl.doDeleteMessage(message, function (messageId) {
            //Go back to the storage list if successful
            ctrl.go("pages.storage.list", { adapter: ctrl.adapterName, storageSource: ctrl.storageSource, storageSourceName: ctrl.storageSourceName, processState: ctrl.processState });
        });
    };
};

appModule.component('storageView', {
    controller: ['$scope', 'Api', '$state', 'SweetAlert', StorageViewController],
    templateUrl: 'js/app/views/storage/storage-view/storage-view.component.html',
});
