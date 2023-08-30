import { appModule } from "../../../app.module";

const IframeLarvaController = function (Misc) {
    const ctrl = this;

    ctrl.$onInit = function () {
        ctrl.url = Misc.getServerPath() + "iaf/larva";
    };
};

appModule.component('iframeLarva', {
    controller: ['Misc',IframeLarvaController],
    templateUrl: 'js/app/views/iframe/iframe.component.html'
});
