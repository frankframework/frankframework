import { Component, Input } from '@angular/core';
import { getProcessStateIcon, getProcessStateIconColor } from '../../../utils';
import { Adapter, Receiver } from '../../../app.service';
import { StatusService } from '../status.service';
import { NgClass, NgIf } from '@angular/common';
import { ToDateDirective } from '../../../components/to-date.directive';
import { TimeSinceDirective } from '../../../components/time-since.directive';
import { HasAccessToLinkDirective } from '../../../components/has-access-to-link.directive';
import { RouterLink } from '@angular/router';
import { TruncatePipe } from '../../../pipes/truncate.pipe';
import { FlowComponent } from '../flow/flow.component';

@Component({
  selector: 'app-adapter-status',
  imports: [
    NgIf,
    NgClass,
    ToDateDirective,
    TimeSinceDirective,
    HasAccessToLinkDirective,
    RouterLink,
    TruncatePipe,
    FlowComponent,
  ],
  templateUrl: './adapter-status.component.html',
  styleUrl: './adapter-status.component.scss',
})
export class AdapterStatusComponent {
  @Input({ required: true }) adapter: Adapter | null = null;
  @Input({ required: true }) loadFlowInline = true;
  @Input({ required: true }) adapterShowContent: Record<string, boolean> = {};

  protected readonly getProcessStateIconColorFn = getProcessStateIconColor;
  protected readonly getProcessStateIconFn = getProcessStateIcon;

  constructor(private statusService: StatusService) {}

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
