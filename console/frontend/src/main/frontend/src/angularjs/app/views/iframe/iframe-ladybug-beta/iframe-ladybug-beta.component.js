import { appModule } from "../../../app.module";

const IframeLadybugBetaController = function (Misc) {
    const ctrl = this;

    ctrl.$onInit = function () {
        ctrl.url = Misc.getServerPath() + "iaf/ladybug";
    };
};

appModule.component('iframeLadybugBeta', {
    controller: ['Misc', IframeLadybugBetaController],
    templateUrl: 'js/app/views/iframe/iframe.component.html'
});
