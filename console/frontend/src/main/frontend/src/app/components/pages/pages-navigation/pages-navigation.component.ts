import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ParamMap, Router, convertToParamMap } from '@angular/router';

@Component({
  selector: 'app-pages-navigation',
  templateUrl: './pages-navigation.component.html',
  styleUrls: ['./pages-navigation.component.scss']
})
export class PagesNavigationComponent {
  @Input() queryParams = convertToParamMap({});
  @Output() onOpenInfo = new EventEmitter<void>();
  @Output() onOpenFeedback = new EventEmitter<void>();

  constructor(private router: Router) { }

  openInfo() {
    this.onOpenInfo.emit();
  }

  openFeedback() {
    this.onOpenFeedback.emit();
  }

  getClassByRoute(className: string, routeState: string | string[]) {
    if (Array.isArray(routeState)) {
      return {
        [className]: routeState.some(routePartial => this.router.url.split("?")[0].includes(routePartial))
      }
    }
    return {
      [className]: this.router.url.split("?")[0].includes(routeState)
    }
  }

  getConfigurationsQueryParam() {
    if (this.queryParams.has('configuration')){
      return this.queryParams.get('configuration');
    }
    return null;
  }
}
