import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'searchFilter',
})
export class SearchFilterPipe implements PipeTransform {
  transform<T>(source: Record<string, T> | T[], searchText: string): Record<string, T> {
    if (Object.keys(source).length === 0) return {};
    if (!searchText || searchText.length === 0) return source;
    const filteredObjectItems: Record<string, T> = {};
    searchText = searchText.toLowerCase();

    for (const itemKey in source) {
      type ItemKey = keyof typeof source;
      if (
        JSON.stringify(source[itemKey as ItemKey])
          .toLowerCase()
          .includes(searchText)
      ) {
        filteredObjectItems[itemKey] = source[itemKey as ItemKey] as T;
      }
    }
    return filteredObjectItems;
  }
}
