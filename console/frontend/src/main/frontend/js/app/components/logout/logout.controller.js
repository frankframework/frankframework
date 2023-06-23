import { appModule } from "../../app.module";

appModule.controller('LogoutCtrl', ['$scope', 'Poller', 'authService', 'Idle', function ($scope, Poller, authService, Idle) {
	Poller.getAll().remove();
	Idle.unwatch();
	authService.logout();
}]);
