import {
  Component,
  EventEmitter,
  Input,
  OnChanges,
  Output,
} from '@angular/core';
import { convertToParamMap, Router, RouterModule } from '@angular/router';
import { MinimalizaSidebarComponent } from './minimaliza-sidebar.component';
import { ScrollToTopComponent } from './scroll-to-top.component';
import { CommonModule } from '@angular/common';
import { CustomViewsComponent } from '../../custom-views/custom-views.component';
import { SideNavigationDirective } from '../side-navigation.directive';

@Component({
  selector: 'app-pages-navigation',
  templateUrl: './pages-navigation.component.html',
  styleUrls: ['./pages-navigation.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    CustomViewsComponent,
    MinimalizaSidebarComponent,
    ScrollToTopComponent,
    SideNavigationDirective,
  ],
})
export class PagesNavigationComponent implements OnChanges {
  @Input() queryParams = convertToParamMap({});
  @Output() shouldOpenInfo = new EventEmitter<void>();
  @Output() shouldOpenFeedback = new EventEmitter<void>();

  protected frankframeworkLogoPath: string = 'assets/images/ff-kawaii.svg';
  protected frankExclamationPath: string =
    'assets/images/frank-exclemation.svg';

  private readonly IMAGES_BASE_PATH = 'assets/images/';

  constructor(private router: Router) {}

  ngOnChanges(): void {
    const uwuEnabledString = localStorage.getItem('uwu') ? 'uwu-' : '';
    this.frankframeworkLogoPath = `${this.IMAGES_BASE_PATH}${uwuEnabledString}frank-framework-logo.svg`;
    this.frankExclamationPath = `${this.IMAGES_BASE_PATH}${uwuEnabledString}frank-exclamation.svg`;
  }

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
