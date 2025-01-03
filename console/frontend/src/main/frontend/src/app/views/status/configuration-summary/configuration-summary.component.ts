import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { AppService, MessageSummary, Summary } from '../../../app.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-configuration-summary',
  templateUrl: './configuration-summary.component.html',
  styleUrl: './configuration-summary.component.scss',
  standalone: false,
})
export class ConfigurationSummaryComponent implements OnInit, OnDestroy {
  @Input({ required: true }) isConfigStubbed: Record<string, boolean> = {};
  @Input({ required: true }) isConfigReloading: Record<string, boolean> = {};
  @Input({ required: true }) isConfigAutoReloadable: Record<string, boolean> = {};
  @Input({ required: true }) selectedConfiguration: string = '';
  @Input({ required: true }) configurationFlowDiagram: string | null = null;
  @Input({ required: true }) reloading: boolean = false;

  protected adapterSummary: Summary = {
    started: 0,
    stopped: 0,
    starting: 0,
    stopping: 0,
    exception_starting: 0,
    exception_stopping: 0,
    error: 0,
  };
  protected receiverSummary: Summary = {
    started: 0,
    stopped: 0,
    starting: 0,
    stopping: 0,
    exception_starting: 0,
    exception_stopping: 0,
    error: 0,
  };
  protected messageSummary: MessageSummary = {
    info: 0,
    warn: 0,
    error: 0,
  };

  private _subscriptions = new Subscription();

  constructor(private appService: AppService) {}

  ngOnInit(): void {
    this.adapterSummary = this.appService.adapterSummary;
    this.receiverSummary = this.appService.receiverSummary;
    this.messageSummary = this.appService.messageSummary;

    const summariesSubscription = this.appService.summaries$.subscribe(() => {
      this.adapterSummary = this.appService.adapterSummary;
      this.receiverSummary = this.appService.receiverSummary;
      this.messageSummary = this.appService.messageSummary;
    });
    this._subscriptions.add(summariesSubscription);
  }

  ngOnDestroy(): void {
    this._subscriptions.unsubscribe();
  }
}
