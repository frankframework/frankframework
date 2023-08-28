import { appModule } from "../../app.module";
import { NotificationService } from "../../services/notification.service";

class PagesTopnavbarController {
  notificationCount: number = 0;
  notificationList: NotificationService["list"] = [];

  constructor(private $scope: angular.IScope, private Notification: NotificationService){}

	$onInit() {
		this.$scope.$watch(() => { return this.Notification.getCount(); }, () => {
			this.notificationCount = this.Notification.getCount();
			this.notificationList = this.Notification.getLatest(5);
		});
	}

	hoverFeedback(rating: number) {
		$(".rating i").removeClass("fa-star").addClass("fa-star-o");
		$(".rating i:nth-child(-n+" + (rating + 1) + ")").addClass("fa-star").removeClass("fa-star-o");
	};

	resetNotificationCount() { this.Notification.resetCount(); };
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
