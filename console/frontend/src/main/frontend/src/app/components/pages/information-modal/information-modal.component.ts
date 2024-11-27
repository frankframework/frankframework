import { Component, OnDestroy, OnInit } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { copyToClipboard } from '../../../utils';
import { ToastService } from '../../../services/toast.service';
import { CommonModule } from '@angular/common';
import { TimeSinceDirective } from '../../time-since.directive';
import { ToDateDirective } from '../../to-date.directive';
import { HumanFileSizePipe } from '../../../pipes/human-file-size.pipe';
import { ServerInfoService } from '../../../services/server-info.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-information-modal',
  templateUrl: './information-modal.component.html',
  styleUrls: ['./information-modal.component.scss'],
  standalone: true,
  imports: [CommonModule, TimeSinceDirective, ToDateDirective, HumanFileSizePipe],
})
export class InformationModalComponent implements OnInit, OnDestroy {
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

  private serverInfoSubscription?: Subscription;

  constructor(
    private activeModal: NgbActiveModal,
    private toastService: ToastService,
    private serverInfoService: ServerInfoService,
  ) {}

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
