import { appModule } from "../../../../app.module";

const FlowModalController = function ($uibModalInstance, xhr) {
    const ctrl = this;

    ctrl.adapter = xhr.adapter;
    ctrl.flow = xhr.data;

    ctrl.close = function () {
        $uibModalInstance.close();
    };
};

appModule.component('flowModal', {
    controller: ['$uibModalInstance', 'xhr', FlowModalController],
    templateUrl: 'js/app/views/status/flow/flow-modal/flow-modal.component.html'
});
