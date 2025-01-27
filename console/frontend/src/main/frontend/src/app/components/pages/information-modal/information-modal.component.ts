import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { copyToClipboard } from '../../../utils';
import { ToastService } from '../../../services/toast.service';

import { TimeSinceDirective } from '../../time-since.directive';
import { ToDateDirective } from '../../to-date.directive';
import { HumanFileSizePipe } from '../../../pipes/human-file-size.pipe';
import { ServerInfoService } from '../../../services/server-info.service';
import { Subscription } from 'rxjs';
import { ServerTimeService } from '../../../services/server-time.service';

@Component({
  selector: 'app-information-modal',
  templateUrl: './information-modal.component.html',
  styleUrls: ['./information-modal.component.scss'],
  imports: [TimeSinceDirective, ToDateDirective, HumanFileSizePipe],
})
export class InformationModalComponent implements OnInit, OnDestroy {
  protected readonly serverTimeService: ServerTimeService = inject(ServerTimeService);
  protected initialized: boolean = false;
  protected framework: {
    name: string;
    version: string;
  } = { name: '', version: '' };
  protected instance: {
    name: string;
    version: string;
  } = { name: '', version: '' };
  protected machineName: string = '';
  protected applicationServer: string = '';
  protected javaVersion: string = '';
  protected processMetrics: {
    maxMemory: number;
    freeMemory: number;
    totalMemory: number;
    heapSize: number;
  } = {
    maxMemory: -1,
    freeMemory: -1,
    totalMemory: -1,
    heapSize: -1,
  };
  protected fileSystem: {
    freeSpace: number;
    totalSpace: number;
  } = {
    freeSpace: -1,
    totalSpace: -1,
  };
  protected uptime: number = 0;

  private activeModal: NgbActiveModal = inject(NgbActiveModal);
  private toastService: ToastService = inject(ToastService);
  private serverInfoService: ServerInfoService = inject(ServerInfoService);
  private serverInfoSubscription?: Subscription;

  ngOnInit(): void {
    this.refresh();
    this.subscribeToServerInfo();
  }

  ngOnDestroy(): void {
    this.serverInfoSubscription?.unsubscribe();
  }

  subscribeToServerInfo(): void {
    this.serverInfoSubscription = this.serverInfoService.serverInfo$.subscribe({
      next: (data) => {
        this.applicationServer = data.applicationServer;
        this.fileSystem = data.fileSystem;
        this.framework = data.framework;
        this.instance = data.instance;
        this.javaVersion = data.javaVersion;
        this.machineName = data.machineName;
        this.processMetrics = data.processMetrics;
        this.uptime = data.uptime;
        this.initialized = true;
      },
    });
  }

  close(): void {
    this.activeModal.close();
  }

  copy(): void {
    copyToClipboard(this.serverInfoService.getMarkdownFormatedServerInfo());
    this.toastService.success('Copied', 'Copied environment information to clipboard');
  }

  refresh(): void {
    this.serverInfoService.refresh().subscribe();
  }
}
