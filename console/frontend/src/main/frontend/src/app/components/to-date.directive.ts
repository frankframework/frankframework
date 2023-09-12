import { Directive, ElementRef, Inject, Input, OnChanges, SimpleChanges } from '@angular/core';
import { APPCONSTANTS } from '../app.module';
import { AppConstants } from 'src/angularjs/app/app.module';
import { AppService } from 'src/angularjs/app/app.service';
import { formatDate } from '@angular/common';

@Directive({
  selector: '[appToDate]'
})
export class ToDateDirective implements OnChanges {
  @Input() time: string | number = "";

  constructor(
    private element: ElementRef,
    private appService: AppService,
    @Inject(APPCONSTANTS) private appConstants: AppConstants,
  ) { }

  ngOnChanges(changes: SimpleChanges) {
    if (isNaN(this.time as number))
      this.time = new Date(this.time).getTime();

    const toDate = new Date((this.time as number) - this.appConstants['timeOffset']);
    this.element.nativeElement.textContent = formatDate(toDate, this.appConstants["console.dateFormat"], this.appService.getUserLocale());
  }

}
