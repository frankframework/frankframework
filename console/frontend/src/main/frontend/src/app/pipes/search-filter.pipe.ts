import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'searchFilter',
})
export class SearchFilterPipe implements PipeTransform {
  transform<T>(source: Record<string, T> | T[], searchText: string): T[] {
    if (Object.keys(source).length === 0) return [];

    if (!searchText || searchText.length === 0) return Object.values(source);
    searchText = searchText.toLowerCase();

    const filtered = Object.values(source).reduce((accumulator, filteredItem) => {
      if (JSON.stringify(filteredItem).replaceAll('"', '').toLowerCase().includes(searchText))
        accumulator.push(filteredItem);
      return accumulator;
    }, [] as T[]);
    return filtered;
  }
}
