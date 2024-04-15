import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Router, convertToParamMap } from '@angular/router';
import { MinimalizaSidebarComponent } from './minimaliza-sidebar.component';
import { ScrollToTopComponent } from './scroll-to-top.component';
import { CommonModule } from '@angular/common';
import { AppRoutingModule } from 'src/app/app-routing.module';
import { CustomViewsComponent } from '../../custom-views/custom-views.component';
import { SideNavigationDirective } from '../side-navigation.directive';

@Component({
  selector: 'app-pages-navigation',
  templateUrl: './pages-navigation.component.html',
  styleUrls: ['./pages-navigation.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    AppRoutingModule,
    CustomViewsComponent,
    MinimalizaSidebarComponent,
    ScrollToTopComponent,
    SideNavigationDirective,
  ],
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
  ): Record<string, boolean> {
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
    return this.queryParams.get('configuration');
  }
}
