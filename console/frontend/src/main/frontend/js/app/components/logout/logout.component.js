import { appModule } from "../../app.module";

const LogoutController = function ($scope, Poller, authService, Idle) {
    const ctrl = this;

    ctrl.$onInit = function () {
        Poller.getAll().remove();
        Idle.unwatch();
        authService.logout();
    };
};

appModule.component('logout', {
    controller: ['$scope', 'Poller', 'authService', 'Idle', LogoutController],
});