import { Component, computed, inject, Input, OnChanges, Signal } from '@angular/core';
import { Notification, NotificationService } from 'src/app/services/notification.service';
import { HamburgerComponent } from './hamburger.component';
import { TimeSinceDirective } from '../../time-since.directive';
import { NgbDropdownModule } from '@ng-bootstrap/ng-bootstrap';
import { RouterModule } from '@angular/router';
import { AuthService } from 'src/app/services/auth.service';
import { AppService, ClusterMember } from 'src/app/app.service';
import { FormsModule } from '@angular/forms';
import { ServerTimeService } from '../../../services/server-time.service';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faGithub } from '@fortawesome/free-brands-svg-icons';
import { faBell } from '@fortawesome/free-solid-svg-icons';

@Component({
  selector: 'app-pages-topnavbar',
  templateUrl: './pages-topnavbar.component.html',
  styleUrls: ['./pages-topnavbar.component.scss'],
  imports: [FormsModule, HamburgerComponent, RouterModule, TimeSinceDirective, NgbDropdownModule, FaIconComponent],
})
export class PagesTopnavbarComponent implements OnChanges {
  @Input() dtapSide = '';
  @Input() dtapStage = '';
  @Input() clusterMembers: ClusterMember[] = [];
  @Input() selectedClusterMember: ClusterMember | null = null;
  @Input() userName?: string;

  protected readonly serverTimeService: ServerTimeService = inject(ServerTimeService);
  protected notificationList: Signal<Notification[]> = computed(() => this.Notification.getLatest(5));
  protected notificationCount: Signal<number>;
  protected loggedIn = false;
  protected readonly faGithub = faGithub;
  protected readonly faBell = faBell;

  private readonly appService: AppService = inject(AppService);
  private readonly authService: AuthService = inject(AuthService);
  private readonly Notification: NotificationService = inject(NotificationService);

  constructor() {
    this.notificationCount = this.Notification.count;
  }

  ngOnChanges(): void {
    this.loggedIn = this.authService.isLoggedIn();
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

  getClusterMemberName(member: ClusterMember): string {
    return member.name;
  }

  getClusterMemberTitle(member: ClusterMember): string {
    return `Name: ${member.name ?? ''}
InstanceName: ${member.attributes.application ?? ''}
ID: ${member.id}
Address: ${member.address}`;
  }
}
