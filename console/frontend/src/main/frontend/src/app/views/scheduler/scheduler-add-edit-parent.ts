import { OnInit } from "@angular/core";
import { Adapter, Configuration } from "src/app/app.service";

interface StateItem {
  type: string
  message: string
}

interface Form {
  name: string
  group: string
  adapter: string
  listener: string
  cron: string
  interval: string
  message: string
  description: string
  locker: boolean
  lockkey: string
}

export class SchedulerAddEditParent {
  state: StateItem[] = [];
  selectedConfiguration: string = "";
  form: Form = {
    name: "",
    group: "",
    adapter: "",
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
    // TODO: button exists, but no function
  }

  addLocalAlert(type: string, message: string) {
    this.state.push({ type: type, message: message });
  };
}
