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

    // @ts-expect-error idk
    array.sort((a, b) => a[field] - b[field]);

    return array;
  }
}
