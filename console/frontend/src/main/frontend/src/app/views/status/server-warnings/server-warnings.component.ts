import { Component, inject, Input } from '@angular/core';
import { ServerInfo } from '../../../services/server-info.service';
import { Alert, MessageLog } from '../../../app.service';
import { Router } from '@angular/router';
import { NgClass } from '@angular/common';
import { HumanFileSizePipe } from '../../../pipes/human-file-size.pipe';
import { SecurityItemsService } from '../../security-items/security-items.service';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faInfoCircle, faTimes, faWarning } from '@fortawesome/free-solid-svg-icons';

@Component({
  selector: 'app-server-warnings',
  templateUrl: './server-warnings.component.html',
  styleUrl: './server-warnings.component.scss',
  imports: [NgClass, HumanFileSizePipe, FaIconComponent],
})
export class ServerWarningsComponent {
  @Input({ required: true }) alerts: Alert[] = [];
  @Input({ required: true }) messageLog: Record<string, MessageLog> = {};
  @Input({ required: true }) selectedConfiguration = 'All';
  @Input() serverInfo: ServerInfo | null = null;
  @Input() freeDiskSpacePercentage: number | null = null;

  protected readonly FREE_DISK_SPACE_ALERT_THRESHOLD = 5;
  protected securityItemsService: SecurityItemsService = inject(SecurityItemsService);

  private router: Router = inject(Router);

  protected navigateByAlert(alert: Alert): void {
    if (alert.link) {
      this.router.navigate(['configuration', alert.link.name], {
        fragment: alert.link['#'],
      });
    }
  }

  protected navigateTo(path: string): void {
    this.router.navigate([path]);
  }

  protected readonly faInfoCircle = faInfoCircle;
  protected readonly faWarning = faWarning;
  protected readonly faTimes = faTimes;
}
