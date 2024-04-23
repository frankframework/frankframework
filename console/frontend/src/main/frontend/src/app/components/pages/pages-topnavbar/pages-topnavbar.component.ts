import { CommonModule } from '@angular/common';
import {
  Component,
  EventEmitter,
  Input,
  OnDestroy,
  OnInit,
  Output,
  Renderer2,
} from '@angular/core';
import { Subscription } from 'rxjs';
import { NotificationService } from 'src/app/services/notification.service';
import { HamburgerComponent } from './hamburger.component';
import { TimeSinceDirective } from '../../time-since.directive';
import { NgbDropdownModule } from '@ng-bootstrap/ng-bootstrap';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-pages-topnavbar',
  templateUrl: './pages-topnavbar.component.html',
  styleUrls: ['./pages-topnavbar.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    HamburgerComponent,
    RouterModule,
    TimeSinceDirective,
    NgbDropdownModule,
  ],
})
export class PagesTopnavbarComponent implements OnInit, OnDestroy {
  notificationCount: number = this.Notification.getCount();
  notificationList: NotificationService['list'] = [];

  @Input() dtapSide: string = '';
  @Input() dtapStage: string = '';
  @Input() serverTime: string = '';
  @Input() loggedin: boolean = false;
  @Input() userName?: string;
  @Output() shouldOpenFeedback = new EventEmitter<number>();

  private _subscriptions = new Subscription();

  constructor(
    private Notification: NotificationService,
    private renderer: Renderer2,
  ) {}

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
    this.shouldOpenFeedback.emit(rating);
  }

  hoverFeedback(rating: number): void {
    const ratingElements = document.querySelectorAll('rating i');
    const selectedRatingElements = document.querySelectorAll(
      `.rating i:nth-child(-n+${rating + 1})`,
    );

    for (const element of ratingElements) {
      this.renderer.removeClass(element, 'fa-star');
      this.renderer.addClass(element, 'fa-star-o');
    }

    for (const element of selectedRatingElements) {
      this.renderer.addClass(element, 'fa-star');
      this.renderer.removeClass(element, 'fa-star-o');
    }
  }

  resetNotificationCount(): void {
    this.Notification.resetCount();
  }
}
