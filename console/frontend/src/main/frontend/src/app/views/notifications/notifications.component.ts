import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { NotificationService, Notification } from 'src/app/services/notification.service';

@Component({
  selector: 'app-notifications',
  imports: [],
  templateUrl: './notifications.component.html',
  styleUrls: ['./notifications.component.scss'],
})
export class NotificationsComponent implements OnInit {
  protected notification: Notification | null = {
    icon: '',
    title: '',
    message: false,
    fn: false,
    time: 0,
  };
  protected text: string = '';

  constructor(
    private route: ActivatedRoute,
    private notificationService: NotificationService,
  ) {}

  ngOnInit(): void {
    this.route.queryParamMap.subscribe((parameters) => {
      const id = parameters.get('id');
      if (id && +id > 0) {
        this.notification = this.notificationService.get(+id);
        console.log('NOTIFICATION LOG:', this.notification);
      } else {
        this.text = 'Showing a list with all notifications!';
      }
    });
  }
}
