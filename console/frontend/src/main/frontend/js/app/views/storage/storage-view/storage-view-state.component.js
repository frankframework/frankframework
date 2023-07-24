import { appModule } from "../../../app.module";

const StorageViewStateController = function ($state) {
    const ctrl = this;

    ctrl.$onInit = function () {
        $state.current.data.breadcrumbs = "Adapter > " + ($state.params.storageSource == 'pipes' ? "Pipes > " + $state.params.storageSourceName + " > " : "") + $state.params.processState + " List > View Message " + $state.params.messageId;
    };
};

appModule.component('storageViewState', {
    controller: ['$state', StorageViewStateController],
});
