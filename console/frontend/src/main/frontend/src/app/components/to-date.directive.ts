import {
  Directive,
  ElementRef,
  Inject,
  Input,
  LOCALE_ID,
  OnChanges,
  OnDestroy,
} from '@angular/core';
import { formatDate } from '@angular/common';
import { AppConstants, AppService } from '../app.service';
import { Subscription } from 'rxjs';

@Directive({
  selector: '[appToDate]',
  standalone: true,
})
export class ToDateDirective implements OnChanges, OnDestroy {
  @Input() time: string | number = '';

  private appConstants: AppConstants;
  private _subscriptions = new Subscription();

  constructor(
    private element: ElementRef,
    private appService: AppService,
    @Inject(LOCALE_ID) private locale: string,
  ) {
    this.appConstants = this.appService.APP_CONSTANTS;
    const appConstantsSubscription = this.appService.appConstants$.subscribe(
      () => {
        this.appConstants = this.appService.APP_CONSTANTS;
      },
    );
    this._subscriptions.add(appConstantsSubscription);
  }

  ngOnChanges(): void {
    if (Number.isNaN(Number(this.time)))
      this.time = new Date(this.time).getTime();

    const toDate = new Date(
      (this.time as number) - (this.appConstants['timeOffset'] as number),
    );
    this.element.nativeElement.textContent = formatDate(
      toDate,
      this.appConstants['console.dateFormat'] as string,
      this.locale,
    );
  }

  ngOnDestroy(): void {
    this._subscriptions.unsubscribe();
  }
}
