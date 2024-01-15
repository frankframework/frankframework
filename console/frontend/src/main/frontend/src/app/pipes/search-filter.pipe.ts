import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'searchFilter'
})
export class SearchFilterPipe implements PipeTransform {
  transform<T>(source: Record<string, T> | T[], searchText: string): T[] {
    if (Object.keys(source).length < 1) return [];

    if (!searchText || searchText.length == 0) return Object.values(source);
    searchText = searchText.toLowerCase();

    const filtered = Object.values(source).reduce((acc, filteredItem) => {
      if (JSON.stringify(filteredItem).replace(/"/g, '').toLowerCase().indexOf(searchText) > -1)
        acc.push(filteredItem);
      return acc;
    }, [] as T[]);
    return filtered;
  }
}
