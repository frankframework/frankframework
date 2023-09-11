import { Directive, ElementRef, Input, OnChanges, SimpleChanges } from '@angular/core';

@Directive({
  selector: '[appToDate]'
})
export class ToDateDirective implements OnChanges {
  @Input() time: string | number = "";

  constructor(private element: ElementRef) { }

  ngOnChanges(changes: SimpleChanges) {
    if (isNaN(this.time as number))
      this.time = new Date(this.time).getTime();
    var toDate = new Date(this.time - this.appConstants.timeOffset);
    this.element.nativeElement.text(dateFilter(toDate, this.appConstants["console.dateFormat"]));
  }

}
