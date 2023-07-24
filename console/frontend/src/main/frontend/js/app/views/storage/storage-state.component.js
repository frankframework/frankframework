import { appModule } from "../../../app.module";

const StorageStateController = function ($state) {
    const ctrl = this;

    ctrl.$onInit = function () {
        $state.current.data.pageTitle = $state.params.processState + " List";
        $state.current.data.breadcrumbs = "Adapter > " + ($state.params.storageSource == 'pipes' ? "Pipes > " + $state.params.storageSourceName + " > " : "") + $state.params.processState + " List";
    };
};

appModule.component('storageState', {
    controller: ['$state', StorageStateController],
    template: "<div ui-view ng-controller='StorageBaseCtrl'></div>",
});
