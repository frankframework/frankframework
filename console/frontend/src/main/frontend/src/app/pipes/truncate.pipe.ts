import { Pipe, PipeTransform } from '@angular/core';
import { copyToClipboard } from '../utils';

@Pipe({
  name: 'truncate',
  standalone: true,
})
export class TruncatePipe implements PipeTransform {
  constructor() {}

  transform(value: string, length: number, onclickElement?: HTMLElement): string {
    if (!(value && value.length > length)) return value;

    if (onclickElement) {
      onclickElement.addEventListener('click', () => {
        copyToClipboard(value);
      });
    }

    return `${value.slice(0, Math.max(0, length))}... (${value.length - length} characters more)`;
  }
}
