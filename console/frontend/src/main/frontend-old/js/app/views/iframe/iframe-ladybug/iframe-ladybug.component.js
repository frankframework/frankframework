import { appModule } from "../../../app.module";

const IframeLadybugController = function ($scope, Misc, $timeout) {
    const ctrl = this;

    ctrl.$onInit = function () {
        ctrl.url = Misc.getServerPath() + "iaf/testtool";
    };
};

appModule.component('iframeLadybug', {
    controller: ['$scope', 'Misc', '$timeout', IframeLadybugController],
    templateUrl: 'js/app/views/iframe/iframe.component.html'
});
