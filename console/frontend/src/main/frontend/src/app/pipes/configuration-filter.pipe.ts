import { Pipe, PipeTransform } from '@angular/core';
import { Adapter, AdapterStatus } from '../app.service';

export function ConfigurationFilter(
  adapters: Record<string, Adapter>,
  selectedConfiguration: string,
  filter?: Record<AdapterStatus, boolean>,
): Record<string, Adapter> {
  if (!adapters || Object.keys(adapters).length === 0) return {};
  const r: Record<string, Adapter> = {};
  for (const adapterName in adapters) {
    const adapter = adapters[adapterName];

    if (
      (adapter.configuration == selectedConfiguration ||
        selectedConfiguration == 'All') &&
      (filter == undefined || filter[adapter.status!])
    )
      r[adapterName] = adapter;
  }
  return r;
}

@Pipe({
  name: 'configurationFilter',
})
export class ConfigurationFilterPipe implements PipeTransform {
  transform(
    adapters: Record<string, Adapter>,
    selectedConfiguration: string,
    filter?: Record<AdapterStatus, boolean>,
  ): Record<string, Adapter> {
    return ConfigurationFilter(adapters, selectedConfiguration, filter);
  }
}
