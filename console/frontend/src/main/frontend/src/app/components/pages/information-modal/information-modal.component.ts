import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { AppService } from 'src/app/app.service';
import { copyToClipboard } from '../../../utils';
import { ToastService } from '../../../services/toast.service';

@Component({
  selector: 'app-information-modal',
  templateUrl: './information-modal.component.html',
  styleUrls: ['./information-modal.component.scss'],
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
    maxMemory: string;
    freeMemory: string;
    totalMemory: string;
    heapSize: string;
  } = {
    maxMemory: '',
    freeMemory: '',
    totalMemory: '',
    heapSize: '',
  };
  fileSystem: {
    freeSpace: string;
    totalSpace: string;
  } = {
    freeSpace: '',
    totalSpace: '',
  };
  uptime: number = 0;

  constructor(
    private activeModal: NgbActiveModal,
    private appService: AppService,
    private toastService: ToastService,
  ) {}

  ngOnInit(): void {
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
}
