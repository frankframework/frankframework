import { Pipe, PipeTransform } from "@angular/core";
import { Adapter, AdapterStatus } from "../app.service";

export function ConfigurationFilter(adapters: Record<string, Adapter>, selectedConfiguration: string, filter?: Record<AdapterStatus, boolean>) {
  if (!adapters || Object.keys(adapters).length < 1) return {};
  let r: Record<string, Adapter> = {};
  for (const adapterName in adapters) {
    let adapter = adapters[adapterName];

    if ((adapter.configuration == selectedConfiguration || selectedConfiguration == "All") && (filter == undefined || filter[adapter.status!]))
      r[adapterName] = adapter;
  }
  return r;
}

@Pipe({
  name: 'configurationFilter'
})
export class ConfigurationFilterPipe implements PipeTransform {
  transform(adapters: Record<string, Adapter>, selectedConfiguration: string, filter?: Record<AdapterStatus, boolean>): Record<string, Adapter> {
    return ConfigurationFilter(adapters, selectedConfiguration, filter);
  }
}
