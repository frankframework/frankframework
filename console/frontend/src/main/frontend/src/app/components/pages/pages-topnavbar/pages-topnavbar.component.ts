import { CommonModule } from '@angular/common';
import { Component, Input, OnChanges, OnDestroy, OnInit, SimpleChanges } from '@angular/core';
import { Subscription } from 'rxjs';
import { NotificationService } from 'src/app/services/notification.service';
import { HamburgerComponent } from './hamburger.component';
import { TimeSinceDirective } from '../../time-since.directive';
import { NgbDropdownModule } from '@ng-bootstrap/ng-bootstrap';
import { RouterModule } from '@angular/router';
import { AuthService } from 'src/app/services/auth.service';
import { AppService, ClusterMember } from 'src/app/app.service';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-pages-topnavbar',
  templateUrl: './pages-topnavbar.component.html',
  styleUrls: ['./pages-topnavbar.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule, HamburgerComponent, RouterModule, TimeSinceDirective, NgbDropdownModule],
})
export class PagesTopnavbarComponent implements OnInit, OnChanges, OnDestroy {
  notificationCount: number = this.Notification.getCount();
  notificationList: NotificationService['list'] = [];

  @Input() dtapSide: string = '';
  @Input() dtapStage: string = '';
  @Input() serverTime: string = '';
  @Input() clusterMembers: ClusterMember[] = [];
  @Input() userName?: string;

  loggedIn: boolean = false;

  protected selectedClusterMember: ClusterMember | null = null;

  private _subscriptions = new Subscription();

  constructor(
    private appService: AppService,
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

  ngOnChanges(changes: SimpleChanges): void {
    this.loggedIn = this.authService.isLoggedIn();

    if (changes['clusterMembers']) {
      this.selectedClusterMember = this.clusterMembers.find((member) => member.selectedMember) ?? null;
    }
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
    return `Name: ${member.name}
ID: ${member.id}
Address: ${member.address}`;
  }
}
