import { Adapter, Configuration } from "src/app/app.service";
import { JobForm } from "./scheduler.service";

interface StateItem {
  type: string
  message: string
}

export class SchedulerAddEditParent {
  state: StateItem[] = [];
  selectedConfiguration: string = "";
  editMode = false;
  form: JobForm = {
    name: "",
    group: "",
    adapter: null,
    listener: "",
    cron: "",
    interval: "",
    message: "",
    description: "",
    locker: false,
    lockkey: "",
  };
  configurations: Configuration[] = [];
  adapters: Record<string, Adapter> = {};

  reset() {
    this.form = {
      name: "",
      group: "",
      adapter: null,
      listener: "",
      cron: "",
      interval: "",
      message: "",
      description: "",
      locker: false,
      lockkey: "",
    };
  }

  addLocalAlert(type: string, message: string) {
    this.state.push({ type: type, message: message });
  };

  clearLocalAlerts(){
    this.state = [];
  }
}
