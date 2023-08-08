import { appModule } from "../../app.module";

const PagesTopnavbarController = function ($scope, Notification) {
	const ctrl = this;

	ctrl.$onInit = function () {
		$scope.$watch(function () { return Notification.getCount(); }, function () {
			ctrl.notificationCount = Notification.getCount();
			ctrl.notificationList = Notification.getLatest(5);
		});
	}

	ctrl.hoverFeedback = function (rating) {
		$(".rating i").removeClass("fa-star").addClass("fa-star-o");
		$(".rating i:nth-child(-n+" + (rating + 1) + ")").addClass("fa-star").removeClass("fa-star-o");
	};

	ctrl.resetNotificationCount = function () { Notification.resetCount(); };
}

appModule.component('pagesTopnavbar', {
	bindings: {
		dtapSide: '<',
		dtapStage: '<',
		serverTime: '<',
		userName: '<',
		loggedin: '<',
		onOpenFeedback: '&'
	},
	controller: ['$scope', 'Notification', PagesTopnavbarController],
  templateUrl: 'angularjs/app/components/pages/pages-topnavbar.component.html'
});
