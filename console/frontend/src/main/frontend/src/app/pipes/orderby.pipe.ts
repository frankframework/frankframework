import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'orderby',
})
/**@Deprecated Replace with component code */
export class OrderByPipe implements PipeTransform {
  transform<T>(array: T[], field: keyof T): T[] {
    if (!array || !field) {
      return array;
    }

    // eslint-disable-next-line unicorn/prefer-simple-sort-comparator
    return array.toSorted((a, b) => {
      if (a[field] < b[field]) {
        return -1;
      }
      if (a[field] > b[field]) {
        return 1;
      }
      return 0;
    });
  }
}
