import { appModule } from "../../../app.module";

const IframeLadybugBetaController = function ($scope, Misc) {
    const ctrl = this;

    ctrl.$onInit = function () {
        ctrl.url = Misc.getServerPath() + "iaf/ladybug";
    };
};

appModule.component('iframeLadybugBeta', {
    controller: ['$scope', 'Misc', IframeLadybugBetaController],
    templateUrl: 'js/app/views/iframe/iframe.component.html'
});
