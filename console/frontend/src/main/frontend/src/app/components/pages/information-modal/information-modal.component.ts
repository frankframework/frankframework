import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { AppService } from 'src/app/app.service';
import { copyToClipboard } from '../../../utils';
import { ToastService } from '../../../services/toast.service';
import { CommonModule } from '@angular/common';
import { TimeSinceDirective } from '../../time-since.directive';
import { ToDateDirective } from '../../to-date.directive';
import { HumanFileSizePipe } from '../../../pipes/human-file-size.pipe';

@Component({
  selector: 'app-information-modal',
  templateUrl: './information-modal.component.html',
  styleUrls: ['./information-modal.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    TimeSinceDirective,
    ToDateDirective,
    HumanFileSizePipe,
  ],
})
export class InformationModalComponent implements OnInit {
  @ViewChild('environmentInformation')
  environmentInformation!: ElementRef<HTMLParagraphElement>;
  error = false;

  framework: {
    name: string;
    version: string;
  } = { name: '', version: '' };
  instance: {
    name: string;
    version: string;
  } = { name: '', version: '' };
  machineName: string = '';
  applicationServer: string = '';
  javaVersion: string = '';
  processMetrics: {
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
  fileSystem: {
    freeSpace: number;
    totalSpace: number;
  } = {
    freeSpace: -1,
    totalSpace: -1,
  };
  uptime: number = 0;

  constructor(
    private activeModal: NgbActiveModal,
    private appService: AppService,
    private toastService: ToastService,
  ) {}

  ngOnInit(): void {
    this.getServerInfo();
  }

  getServerInfo(): void {
    this.appService.getServerInfo().subscribe({
      next: (data) => {
        this.applicationServer = data.applicationServer;
        this.fileSystem = data.fileSystem;
        this.framework = data.framework;
        this.instance = data.instance;
        this.javaVersion = data.javaVersion;
        this.machineName = data.machineName;
        this.processMetrics = data.processMetrics;
        this.uptime = data.uptime;
      },
      error: () => {
        this.error = true;
      },
    });
  }

  close(): void {
    this.activeModal.close();
  }

  copy(): void {
    copyToClipboard(this.environmentInformation.nativeElement.innerText); // Needs to be innerText to copy newlines.
    this.toastService.success(
      'Copied',
      'Copied environment information to clipboard',
    );
  }

  refresh(): void {
    this.getServerInfo();
  }
}
