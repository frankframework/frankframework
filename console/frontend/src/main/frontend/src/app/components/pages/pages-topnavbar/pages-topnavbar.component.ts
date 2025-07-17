import { Component, inject, Input, OnChanges, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { NotificationService, Notification } from 'src/app/services/notification.service';
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
  imports: [FormsModule, HamburgerComponent, RouterModule, TimeSinceDirective, NgbDropdownModule],
})
export class PagesTopnavbarComponent implements OnInit, OnChanges, OnDestroy {
  @Input() dtapSide: string = '';
  @Input() dtapStage: string = '';
  @Input() clusterMembers: ClusterMember[] = [];
  @Input() selectedClusterMember: ClusterMember | null = null;
  @Input() userName?: string;

  protected readonly serverTimeService: ServerTimeService = inject(ServerTimeService);
  protected notificationCount: number = 0;
  protected notificationList: Notification[] = [];
  protected loggedIn: boolean = false;

  private readonly appService: AppService = inject(AppService);
  private readonly authService: AuthService = inject(AuthService);
  private readonly Notification: NotificationService = inject(NotificationService);
  private notificationCountSubscription: Subscription | null = null;

  ngOnInit(): void {
    this.notificationCountSubscription = this.Notification.onCountUpdate$.subscribe(() => {
      this.notificationCount = this.Notification.getCount();
      this.notificationList = this.Notification.getLatest(5);
    });
  }

  ngOnChanges(): void {
    this.loggedIn = this.authService.isLoggedIn();
  }

  ngOnDestroy(): void {
    this.notificationCountSubscription?.unsubscribe();
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
