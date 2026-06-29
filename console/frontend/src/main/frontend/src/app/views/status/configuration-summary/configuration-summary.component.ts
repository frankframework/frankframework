import { Component, inject, Input, OnChanges, Signal, ChangeDetectionStrategy } from '@angular/core';
import { AppService, MessageSummary, Summary } from '../../../app.service';
import { NgClass } from '@angular/common';
import { FlowComponent } from '../flow/flow.component';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faCheckSquare, faStopCircle, faWarning } from '@fortawesome/free-solid-svg-icons';
import { faCheckSquare as faCheckSquareO, faStopCircle as faStopCircleO } from '@fortawesome/free-regular-svg-icons';

@Component({
  selector: 'app-configuration-summary',
  templateUrl: './configuration-summary.component.html',
  styleUrl: './configuration-summary.component.scss',
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [NgClass, FlowComponent, FaIconComponent],
})
export class ConfigurationSummaryComponent implements OnChanges {
  @Input({ required: true }) isConfigStubbed: Record<string, boolean> = {};
  @Input({ required: true }) isConfigReloading: Record<string, boolean> = {};
  @Input({ required: true }) isConfigAutoReloadable: Record<string, boolean> = {};
  @Input({ required: true }) selectedConfiguration = '';
  @Input({ required: true }) configurationFlowDiagram: string | null = null;
  @Input({ required: true }) reloading = false;

  protected title = 'Configuration';
  protected readonly adapterSummarySignal: Signal<Summary>;
  protected readonly receiverSummarySignal: Signal<Summary>;
  protected readonly messageSummarySignal: Signal<MessageSummary>;
  protected readonly faCheckSquare = faCheckSquare;
  protected readonly faCheckSquareO = faCheckSquareO;
  protected readonly faStopCircle = faStopCircle;
  protected readonly faStopCircleO = faStopCircleO;
  protected readonly faWarning = faWarning;

  private readonly appService: AppService = inject(AppService);

  constructor() {
    this.adapterSummarySignal = this.appService.adapterSummary;
    this.receiverSummarySignal = this.appService.receiverSummary;
    this.messageSummarySignal = this.appService.messageSummary;
  }

  ngOnChanges(): void {
    this.title = this.selectedConfiguration === 'All' ? 'Application' : 'Configuration';
  }
}
