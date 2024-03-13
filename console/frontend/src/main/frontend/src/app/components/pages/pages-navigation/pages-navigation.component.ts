import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Router, convertToParamMap } from '@angular/router';

@Component({
  selector: 'app-pages-navigation',
  templateUrl: './pages-navigation.component.html',
  styleUrls: ['./pages-navigation.component.scss'],
})
export class PagesNavigationComponent {
  @Input() queryParams = convertToParamMap({});
  @Output() shouldOpenInfo = new EventEmitter<void>();
  @Output() shouldOpenFeedback = new EventEmitter<void>();

  constructor(private router: Router) {}

  openInfo(): void {
    this.shouldOpenInfo.emit();
  }

  openFeedback(): void {
    this.shouldOpenFeedback.emit();
  }

  getClassByRoute(
    className: string,
    routeState: string | string[],
  ): {
    [x: string]: boolean;
  } {
    if (Array.isArray(routeState)) {
      return {
        [className]: routeState.some((routePartial) =>
          this.router.url.split('?')[0].includes(routePartial),
        ),
      };
    }
    return {
      [className]: this.router.url.split('?')[0].includes(routeState),
    };
  }

  getConfigurationsQueryParam(): string | null {
    if (this.queryParams.has('configuration')) {
      return this.queryParams.get('configuration');
    }
    return null;
  }
}
