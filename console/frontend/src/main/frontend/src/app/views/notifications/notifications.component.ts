import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { NotificationService, Notification } from 'src/app/services/notification.service';

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
    private route: ActivatedRoute,
    private notificationService: NotificationService,
  ) { };

  ngOnInit(): void {
    this.route.queryParamMap.subscribe(params => {
      const id = params.get('id');
      if (id && +id > 0) {
        this.notification = this.notificationService.get(+id);
        console.log("NOTIFICATION LOG:", this.notification)
      } else {
        this.text = ("Showing a list with all notifications!");
      }
    });

    // this.hooksService.register("adapterUpdated:2", function (adapter: Adapter) {
    //   console.warn("What is the scope of: ", adapter);
    // });
  };
}
