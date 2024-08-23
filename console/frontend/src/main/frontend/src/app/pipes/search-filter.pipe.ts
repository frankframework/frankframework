import { Pipe, PipeTransform } from '@angular/core';

type Source<T> = Record<string, T> | T[];

@Pipe({
  name: 'searchFilter',
})
export class SearchFilterPipe implements PipeTransform {
  transform<T>(source: T[], searchText: string): T[];
  transform<T>(source: Record<string, T>, searchText: string): Record<string, T>;
  transform<T>(source: Source<T>, searchText: string): Source<T> {
    if (!searchText || searchText.length === 0) return source;
    if (Array.isArray(source)) return this.transformArray(source, searchText);
    return this.transformRecord(source, searchText);
  }

  private transformArray<T>(source: T[], searchText: string): T[] {
    if (source.length === 0) return source;
    return source.filter((item) => {
      return this.matchSearchText(item, searchText);
    });
  }

  private transformRecord<T>(source: Record<string, T>, searchText: string): Record<string, T> {
    if (Object.keys(source).length === 0) return {};
    const filteredObjectItems: Record<string, T> = {};
    for (const itemKey in source) {
      if (this.matchSearchText(source[itemKey], searchText)) {
        filteredObjectItems[itemKey] = source[itemKey];
      }
    }
    return filteredObjectItems;
  }

  private matchSearchText<T>(value: T, searchText: string): boolean {
    return JSON.stringify(value).toLowerCase().includes(searchText.toLowerCase());
  }
}
