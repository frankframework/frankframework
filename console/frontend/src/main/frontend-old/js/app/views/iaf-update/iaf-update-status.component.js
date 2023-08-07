import { appModule } from "../../app.module";

const IafUpdateStatusController = function ($scope, $location, Session) {
	const ctrl = this;

	ctrl.$onInit = function () {
		ctrl.release = Session.get("IAF-Release");
		if (ctrl.release == undefined)
			$location.path("status");
	};
}

appModule.component('iafUpdateStatus', {
	controller: ['$scope', '$location', 'Session', IafUpdateStatusController],
	templateUrl: 'js/app/views/iaf-update/iaf-update-status.component.html'
});
