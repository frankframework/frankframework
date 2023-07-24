import { appModule } from "../../../app.module";

const IframeLarvaController = function ($scope, Misc, $interval) {
    const ctrl = this;

    ctrl.$onInit = function () {
        ctrl.url = Misc.getServerPath() + "iaf/larva";
    };
};

appModule.component('iframeLarva', {
    controller: ['$scope', 'Misc', '$interval', IframeLarvaController],
    templateUrl: 'js/app/views/iframe/iframe.component.html'
});
