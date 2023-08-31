import { Component, EventEmitter, Input, Output } from '@angular/core';
import { NotificationService } from 'src/angularjs/app/services/notification.service';

@Component({
  selector: 'app-pages-topnavbar',
  templateUrl: './pages-topnavbar.component.html',
  styleUrls: ['./pages-topnavbar.component.scss']
})
export class PagesTopnavbarComponent {
  notificationCount: number = 0;
  notificationList: NotificationService["list"] = [];

  @Input() dtapSide: string = "";
  @Input() dtapStage: string = "";
  @Input() serverTime: string = "";
  @Input() userName: string = "";
  @Input() loggedin: boolean = false;
  @Output() onOpenFeedback = new EventEmitter<number>();

  constructor(private Notification: NotificationService) { }

  $onInit() {
    this.Notification.onCountUpdate$.subscribe(() => {
      this.notificationCount = this.Notification.getCount();
      this.notificationList = this.Notification.getLatest(5);
    });
  }

  openFeedback(rating: number){
    this.onOpenFeedback.emit(rating);
  }

  hoverFeedback(rating: number) {
    $(".rating i").removeClass("fa-star").addClass("fa-star-o");
    $(".rating i:nth-child(-n+" + (rating + 1) + ")").addClass("fa-star").removeClass("fa-star-o");
  };

  resetNotificationCount() { this.Notification.resetCount(); };
}
