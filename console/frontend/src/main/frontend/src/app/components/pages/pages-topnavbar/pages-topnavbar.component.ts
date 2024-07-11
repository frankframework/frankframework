import { CommonModule } from '@angular/common';
import { Component, Input, OnChanges, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { NotificationService } from 'src/app/services/notification.service';
import { HamburgerComponent } from './hamburger.component';
import { TimeSinceDirective } from '../../time-since.directive';
import { NgbDropdownModule } from '@ng-bootstrap/ng-bootstrap';
import { RouterModule } from '@angular/router';
import { AuthService } from 'src/app/services/auth.service';

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
export class PagesTopnavbarComponent implements OnInit, OnChanges, OnDestroy {
  notificationCount: number = this.Notification.getCount();
  notificationList: NotificationService['list'] = [];

  @Input() dtapSide: string = '';
  @Input() dtapStage: string = '';
  @Input() serverTime: string = '';
  @Input() userName?: string;

  loggedIn: boolean = false;

  private _subscriptions = new Subscription();

  constructor(
    private Notification: NotificationService,
    private authService: AuthService,
  ) {}

  ngOnInit(): void {
    const notifCountSub = this.Notification.onCountUpdate$.subscribe(() => {
      this.notificationCount = this.Notification.getCount();
      this.notificationList = this.Notification.getLatest(5);
    });
    this._subscriptions.add(notifCountSub);
  }

  ngOnChanges(): void {
    this.loggedIn = this.authService.isLoggedIn();
  }

  ngOnDestroy(): void {
    this._subscriptions.unsubscribe();
  }

  resetNotificationCount(): void {
    this.Notification.resetCount();
  }
}
