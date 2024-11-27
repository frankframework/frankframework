import { CommonModule } from '@angular/common';
import { Component, inject, Input, OnChanges, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { NotificationService } from 'src/app/services/notification.service';
import { HamburgerComponent } from './hamburger.component';
import { TimeSinceDirective } from '../../time-since.directive';
import { NgbDropdownModule } from '@ng-bootstrap/ng-bootstrap';
import { RouterModule } from '@angular/router';
import { AuthService } from 'src/app/services/auth.service';
import { AppService, ClusterMember } from 'src/app/app.service';
import { FormsModule } from '@angular/forms';
import { ServerTimeService } from '../../../services/server-time.service';

@Component({
  selector: 'app-pages-topnavbar',
  templateUrl: './pages-topnavbar.component.html',
  styleUrls: ['./pages-topnavbar.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule, HamburgerComponent, RouterModule, TimeSinceDirective, NgbDropdownModule],
})
export class PagesTopnavbarComponent implements OnInit, OnChanges, OnDestroy {
  @Input() dtapSide: string = '';
  @Input() dtapStage: string = '';
  @Input() clusterMembers: ClusterMember[] = [];
  @Input() selectedClusterMember: ClusterMember | null = null;
  @Input() userName?: string;

  private Notification: NotificationService = inject(NotificationService);

  protected serverTimeService: ServerTimeService = inject(ServerTimeService);
  protected notificationCount: number = this.Notification.getCount();
  protected notificationList: NotificationService['list'] = [];
  protected loggedIn: boolean = false;

  private appService: AppService = inject(AppService);
  private authService: AuthService = inject(AuthService);
  private _subscriptions = new Subscription();

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

  selectClusterMember(): void {
    if (this.selectedClusterMember) {
      this.appService.updateSelectedClusterMember(this.selectedClusterMember.id).subscribe(() => {
        this.appService.triggerReload();
      });
    }
  }

  getClusterMemberTitle(member: ClusterMember): string {
    return `Name: ${member.attributes.name ?? ''}
ID: ${member.id}
Address: ${member.address}`;
  }
}
