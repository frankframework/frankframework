import { Adapter, AppService, Configuration } from 'src/app/app.service';
import { JobForm } from './scheduler.service';
import { inject, Signal } from '@angular/core';

interface StateItem {
  type: string;
  message: string;
}

export class SchedulerAddEditParent {
  protected state: StateItem[] = [];
  protected selectedConfiguration: string = '';
  protected editMode = false;
  protected form: JobForm = {
    name: '',
    group: '',
    adapter: null,
    listener: '',
    cron: '',
    interval: '',
    message: '',
    description: '',
    locker: false,
    lockkey: '',
  };

  private readonly appService: AppService = inject(AppService);
  protected configurations: Signal<Configuration[]> = this.appService.configurations;
  protected adapters: Signal<Record<string, Adapter>> = this.appService.adapters;

  reset(): void {
    this.form = {
      name: '',
      group: '',
      adapter: null,
      listener: '',
      cron: '',
      interval: '',
      message: '',
      description: '',
      locker: false,
      lockkey: '',
    };
  }

  addLocalAlert(type: string, message: string): void {
    this.state.push({ type: type, message: message });
  }

  clearLocalAlerts(): void {
    this.state = [];
  }
}
