import { appModule } from "../../../app.module";

const IframeLadybugController = function (Misc) {
    const ctrl = this;

    ctrl.$onInit = function () {
        ctrl.url = Misc.getServerPath() + "iaf/testtool";
    };
};

appModule.component('iframeLadybug', {
    controller: ['Misc', IframeLadybugController],
    templateUrl: 'js/app/views/iframe/iframe.component.html'
});
