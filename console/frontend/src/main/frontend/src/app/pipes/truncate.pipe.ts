import { ElementRef, Pipe, PipeTransform } from '@angular/core';
import { AppService } from '../app.service';

@Pipe({
  name: 'truncate'
})
export class TruncatePipe implements PipeTransform {

  constructor(private appService: AppService) { }

  transform(value: string, length: number, onclickElement?: HTMLElement): string {
    if (!(value && value.length > length))
      return value;

    if (onclickElement){
      onclickElement.addEventListener('click', () => {
        this.appService.copyToClipboard(value);
      });
    }

    return value.substring(0, length) + "... (" + (value.length - length) + " characters more)";
  }
}
