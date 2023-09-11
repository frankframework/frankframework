import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'orderby'
})
/**@Deprecated Replace with component code */
export class OrderByPipe implements PipeTransform {
  transform(array: any[], field: string): any[] {
    if (!array || !field) {
      return array;
    }

    array.sort((a: any, b: any) => {
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
