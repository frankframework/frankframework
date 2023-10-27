import { Pipe, PipeTransform } from '@angular/core';
import { Adapter } from '../app.service';

@Pipe({
  name: 'searchFilter'
})
export class SearchFilterPipe implements PipeTransform {
  transform(adapters: Record<string, Adapter>, searchText: string): Adapter[] {
    if (Object.keys(adapters).length < 1) return [];

    if (!searchText || searchText.length == 0) return Object.values(adapters);
    searchText = searchText.toLowerCase();

    const filtered = Object.values(adapters).reduce((acc, adapter) => {
      if (JSON.stringify(adapter).replace(/"/g, '').toLowerCase().indexOf(searchText) > -1)
        acc.push(adapter);
      return acc;
    }, [] as Adapter[]);
    return filtered;
  }
}
