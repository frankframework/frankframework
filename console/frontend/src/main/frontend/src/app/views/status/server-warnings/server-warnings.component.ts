import { Component, inject, Input } from '@angular/core';
import { ServerInfo } from '../../../services/server-info.service';
import { Alert, MessageLog } from '../../../app.service';
import { Router } from '@angular/router';
import { NgClass } from '@angular/common';
import { HumanFileSizePipe } from '../../../pipes/human-file-size.pipe';
import { SecurityItemsService } from '../../security-items/security-items.service';

@Component({
  selector: 'app-server-warnings',
  templateUrl: './server-warnings.component.html',
  styleUrl: './server-warnings.component.scss',
  imports: [NgClass, HumanFileSizePipe],
})
export class ServerWarningsComponent {
  @Input({ required: true }) alerts: Alert[] = [];
  @Input({ required: true }) messageLog: Record<string, MessageLog> = {};
  @Input({ required: true }) selectedConfiguration: string = 'All';
  @Input() serverInfo: ServerInfo | null = null;
  @Input() freeDiskSpacePercentage?: number;

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
}
