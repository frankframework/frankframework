import {
  Component,
  EventEmitter,
  Input,
  OnDestroy,
  OnInit,
  Output,
} from '@angular/core';
import { Subscription } from 'rxjs';
import { NotificationService } from 'src/app/services/notification.service';

@Component({
  selector: 'app-pages-topnavbar',
  templateUrl: './pages-topnavbar.component.html',
  styleUrls: ['./pages-topnavbar.component.scss'],
})
export class PagesTopnavbarComponent implements OnInit, OnDestroy {
  notificationCount: number = this.Notification.getCount();
  notificationList: NotificationService['list'] = [];

  @Input() dtapSide: string = '';
  @Input() dtapStage: string = '';
  @Input() serverTime: string = '';
  @Input() loggedin: boolean = false;
  @Input() userName?: string;
  @Output() onOpenFeedback = new EventEmitter<number>();

  private _subscriptions = new Subscription();

  constructor(private Notification: NotificationService) {}

  ngOnInit(): void {
    const notifCountSub = this.Notification.onCountUpdate$.subscribe(() => {
      this.notificationCount = this.Notification.getCount();
      this.notificationList = this.Notification.getLatest(5);
    });
    this._subscriptions.add(notifCountSub);
  }

  ngOnDestroy(): void {
    this._subscriptions.unsubscribe();
  }

  openFeedback(rating: number): void {
    this.onOpenFeedback.emit(rating);
  }

  hoverFeedback(rating: number): void {
    $('.rating i').removeClass('fa-star').addClass('fa-star-o');
    $(`.rating i:nth-child(-n+${rating + 1})`)
      .addClass('fa-star')
      .removeClass('fa-star-o');
  }

  resetNotificationCount(): void {
    this.Notification.resetCount();
  }
}
