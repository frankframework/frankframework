import { appModule } from "../../app.module";

const LoginController = function (authService, $timeout, Alert) {
    const ctrl = this;

    ctrl.credentials = {};

    ctrl.$onInit = function () {
        authService.loggedin(); //Check whether or not the client is logged in.

        $timeout(function () {
            ctrl.notifications = Alert.get();
            angular.element(".main").show();
            angular.element(".loading").hide();
            angular.element("body").addClass("gray-bg");
        }, 500);
    };

    ctrl.login = function (credentials) {
        authService.login(credentials.username, credentials.password);
    };
};

appModule.component('login', {
    controller: ['authService', '$timeout', 'Alert', LoginController],
    templateUrl: 'js/app/views/login/login.component.html',
});
