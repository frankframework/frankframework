import { Component, Input } from '@angular/core';
import { ServerInfo } from '../../../services/server-info.service';
import { Alert, MessageLog } from '../../../app.service';
import { Router } from '@angular/router';
import { NgClass, NgFor, NgIf } from '@angular/common';
import { HumanFileSizePipe } from '../../../pipes/human-file-size.pipe';

@Component({
  selector: 'app-server-warnings',
  templateUrl: './server-warnings.component.html',
  styleUrl: './server-warnings.component.scss',
  imports: [NgIf, NgClass, HumanFileSizePipe, NgFor],
})
export class ServerWarningsComponent {
  @Input({ required: true }) alerts: Alert[] = [];
  @Input({ required: true }) messageLog: Record<string, MessageLog> = {};
  @Input({ required: true }) selectedConfiguration: string = 'All';
  @Input() freeDiskSpacePercentage?: number;
  @Input() serverInfo?: ServerInfo;

  protected readonly FREE_DISK_SPACE_ALERT_THRESHOLD = 5;

  constructor(private router: Router) {}

  navigateByAlert(alert: Alert): void {
    if (alert.link) {
      this.router.navigate(['configuration', alert.link.name], {
        fragment: alert.link['#'],
      });
    }
  }
}
