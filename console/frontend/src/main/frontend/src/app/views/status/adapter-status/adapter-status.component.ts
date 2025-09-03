import { Component, inject, Input } from '@angular/core';
import { getProcessStateIcon, getProcessStateIconColor } from '../../../utilities';
import { Adapter, Receiver } from '../../../app.service';
import { StatusService } from '../status.service';
import { NgClass } from '@angular/common';
import { ToDateDirective } from '../../../components/to-date.directive';
import { TimeSinceDirective } from '../../../components/time-since.directive';
import { HasAccessToLinkDirective } from '../../../components/has-access-to-link.directive';
import { RouterLink } from '@angular/router';
import { TruncatePipe } from '../../../pipes/truncate.pipe';
import { FlowComponent } from '../flow/flow.component';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import {
  faBarChart,
  faChevronDown,
  faChevronUp,
  faCog,
  faDatabase,
  faExclamationTriangle,
  faInfo,
  faMinus,
  faPlay,
  faPlus,
  faSignIn,
  faSignOut,
  faStop,
  faTimes,
  faTimesCircle,
  faWarning,
  faCheckSquare,
  faStopCircle,
} from '@fortawesome/free-solid-svg-icons';
import { faCheckSquare as faCheckSquareO, faStopCircle as faStopCircleO } from '@fortawesome/free-regular-svg-icons';

@Component({
  selector: 'app-adapter-status',
  imports: [
    NgClass,
    ToDateDirective,
    TimeSinceDirective,
    HasAccessToLinkDirective,
    RouterLink,
    TruncatePipe,
    FlowComponent,
    FaIconComponent,
  ],
  templateUrl: './adapter-status.component.html',
  styleUrl: './adapter-status.component.scss',
})
export class AdapterStatusComponent {
  @Input({ required: true }) adapter!: Adapter;
  @Input({ required: true }) loadFlowInline = true;
  @Input({ required: true }) adapterShowContent: Record<string, boolean> = {};

  protected readonly getProcessStateIconColorFn = getProcessStateIconColor;
  protected readonly getProcessStateIconFn = getProcessStateIcon;
  protected readonly faChevronUp = faChevronUp;
  protected readonly faChevronDown = faChevronDown;
  protected readonly faCog = faCog;
  protected readonly faExclamationTriangle = faExclamationTriangle;
  protected readonly faSignIn = faSignIn;
  protected readonly faTimesCircle = faTimesCircle;
  protected readonly faDatabase = faDatabase;
  protected readonly faSignOut = faSignOut;
  protected readonly faBarChart = faBarChart;
  protected readonly faCheckSquare = faCheckSquare;
  protected readonly faCheckSquareO = faCheckSquareO;
  protected readonly faStop = faStop;
  protected readonly faStopCircle = faStopCircle;
  protected readonly faStopCircleO = faStopCircleO;
  protected readonly faPlay = faPlay;
  protected readonly faWarning = faWarning;
  protected readonly faPlus = faPlus;
  protected readonly faMinus = faMinus;
  protected readonly faInfo = faInfo;
  protected readonly faTimes = faTimes;

  private statusService = inject(StatusService);

  protected showContent(adapter: Adapter): boolean {
    return this.adapterShowContent[`${adapter.configuration}/${adapter.name}`];
  }

  protected getTransactionalStores(receiver: Receiver): { name: string; numberOfMessages: number }[] {
    return Object.values(receiver.transactionalStores);
  }

  protected startAdapter(adapter: Adapter): void {
    adapter.state = 'starting';
    this.statusService.updateAdapter(adapter.configuration, adapter.name, 'start').subscribe();
  }

  protected stopAdapter(adapter: Adapter): void {
    adapter.state = 'stopping';
    this.statusService.updateAdapter(adapter.configuration, adapter.name, 'stop').subscribe();
  }

  protected startReceiver(adapter: Adapter, receiver: Receiver): void {
    receiver.state = 'loading';
    this.statusService.updateReceiver(adapter.configuration, adapter.name, receiver.name, 'start').subscribe();
  }

  protected stopReceiver(adapter: Adapter, receiver: Receiver): void {
    receiver.state = 'loading';
    this.statusService.updateReceiver(adapter.configuration, adapter.name, receiver.name, 'stop').subscribe();
  }

  protected addThread(adapter: Adapter, receiver: Receiver): void {
    receiver.state = 'loading';
    this.statusService.updateReceiver(adapter.configuration, adapter.name, receiver.name, 'incthread').subscribe();
  }

  protected removeThread(adapter: Adapter, receiver: Receiver): void {
    receiver.state = 'loading';
    this.statusService.updateReceiver(adapter.configuration, adapter.name, receiver.name, 'decthread').subscribe();
  }
}
