import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'orderby'
})
/**@Deprecated Replace with component code */
export class OrderByPipe implements PipeTransform {
  transform<T extends Record<string, U>, U>(array: T[], field: keyof T): T[] {
    if (!array || !field) {
      return array;
    }

    array.sort((a, b) => {
      if (a[field] < b[field]) {
        return -1;
      }
      if (a[field] > b[field]) {
        return 1;
      }
      return 0;
    });

    return array;
  }
}
