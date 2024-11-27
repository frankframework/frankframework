import { Adapter, Configuration } from 'src/app/app.service';
import { JobForm } from './scheduler.service';

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
  protected configurations: Configuration[] = [];
  protected adapters: Record<string, Adapter> = {};

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
