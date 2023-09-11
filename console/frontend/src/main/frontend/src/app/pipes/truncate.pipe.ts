import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'truncate'
})
export class TruncatePipe implements PipeTransform {
  transform(value: string, length: number): string {
    if (value && value.length > length) {
      return value.substring(0, length) + "... (" + (value.length - length) + " characters more)";
    }
    return value;
  }
}
