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
  @Input({ required: true }) selectedConfiguration: string = '';
  @Input({ required: true }) configurationFlowDiagram: string | null = null;
  @Input({ required: true }) reloading: boolean = false;

  private appService: AppService = inject(AppService);
  protected adapterSummarySignal: Signal<Summary> = this.appService.adapterSummary;
  protected receiverSummarySignal: Signal<Summary> = this.appService.receiverSummary;
  protected messageSummarySignal: Signal<MessageSummary> = this.appService.messageSummary;
}
