import { appModule } from "../../app.module";
import { NotificationService } from "../../services/notification.service";
import { StateParams } from '@uirouter/angularjs';
import { HooksService } from "src/app/services.types";

class NotificationsController {
  notification: any;
  text: string = "";

  constructor(
    private stateParams: StateParams,
    private Notification: NotificationService,
  ) { };

  $onInit() {
    if (this.stateParams['id'] > 0) {
      this.notification = this.Notification.get(this.stateParams['id']);
    } else {
      this.text = ("Showing a list with all notifications!");
    }

    // TODO: HooksService not used anymore
    // this.hooksService.register("adapterUpdated:2", function (adapter: Adapter) {
    //   console.warn("What is the scope of: ", adapter);
    // });
  };
};

appModule.component('notifications', {
  controller: ['$stateParams', 'Hooks', 'Notification', NotificationsController],
  templateUrl: 'js/app/views/notifications/notifications.component.html'
});
