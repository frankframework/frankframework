import { Component, inject, Input, Signal } from '@angular/core';
import { AppService, MessageSummary, Summary } from '../../../app.service';
import { NgClass } from '@angular/common';
import { FlowComponent } from '../flow/flow.component';

@Component({
  selector: 'app-configuration-summary',
  templateUrl: './configuration-summary.component.html',
  styleUrl: './configuration-summary.component.scss',
  imports: [NgClass, FlowComponent],
})
export class ConfigurationSummaryComponent {
  @Input({ required: true }) isConfigStubbed: Record<string, boolean> = {};
  @Input({ required: true }) isConfigReloading: Record<string, boolean> = {};
  @Input({ required: true }) isConfigAutoReloadable: Record<string, boolean> = {};
  @Input({ required: true }) selectedConfiguration = '';
  @Input({ required: true }) configurationFlowDiagram: string | null = null;
  @Input({ required: true }) reloading = false;

  protected adapterSummarySignal: Signal<Summary>;
  protected receiverSummarySignal: Signal<Summary>;
  protected messageSummarySignal: Signal<MessageSummary>;

  private appService: AppService = inject(AppService);

  constructor() {
    this.adapterSummarySignal = this.appService.adapterSummary;
    this.receiverSummarySignal = this.appService.receiverSummary;
    this.messageSummarySignal = this.appService.messageSummary;
  }
}
