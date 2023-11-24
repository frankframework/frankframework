import { Component, OnInit } from '@angular/core';
import { StateParams } from '@uirouter/angularjs';
import { Notification, NotificationService } from 'src/angularjs/app/services/notification.service';
import { Adapter } from 'src/app/app.service';
import { HooksService } from 'src/app/services.types';

@Component({
  selector: 'app-notifications',
  templateUrl: './notifications.component.html',
  styleUrls: ['./notifications.component.scss']
})
export class NotificationsComponent implements OnInit {
  notification: Notification | false = {
    icon: "",
    title: "",
    message: false,
    fn: false,
    time: 0,
  };
  text: string = "";

  constructor(
    private stateParams: StateParams,
    // private hooksService: HooksService,
    private notificationService: NotificationService,
  ) { };

  ngOnInit(): void {
    if (this.stateParams['id'] > 0) {
      this.notification = this.notificationService.get(this.stateParams['id']);
      console.log("NOTIFICATION LOG:", this.notification)
    } else {
      this.text = ("Showing a list with all notifications!");
    }

    // TODO: HooksService not used anymore
    // this.hooksService.register("adapterUpdated:2", function (adapter: Adapter) {
    //   console.warn("What is the scope of: ", adapter);
    // });
  };
}
