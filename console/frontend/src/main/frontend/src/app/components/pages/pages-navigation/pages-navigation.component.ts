import { Component, EventEmitter, Input, OnChanges, OnInit, Output } from '@angular/core';
import { convertToParamMap, Router, RouterModule } from '@angular/router';
import { MinimalizaSidebarComponent } from './minimaliza-sidebar.component';
import { ScrollToTopComponent } from './scroll-to-top.component';
import { CommonModule } from '@angular/common';
import { CustomViewsComponent } from '../../custom-views/custom-views.component';
import { SideNavigationDirective } from '../side-navigation.directive';
import { InformationModalComponent } from '../information-modal/information-modal.component';
import { ServerInfoService } from '../../../services/server-info.service';

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
    InformationModalComponent,
  ],
})
export class PagesNavigationComponent implements OnChanges, OnInit {
  @Input() queryParams = convertToParamMap({});
  @Output() shouldOpenInfo = new EventEmitter<void>();

  protected frankframeworkLogoPath: string = 'assets/images/ff-kawaii.svg';
  protected frankExclamationPath: string = 'assets/images/frank-exclemation.svg';
  protected encodedServerInfo: string = '';

  private readonly IMAGES_BASE_PATH = 'assets/images/';

  constructor(
    private router: Router,
    private serverInfoService: ServerInfoService,
  ) {}

  ngOnInit(): void {
    this.serverInfoService.serverInfo$.subscribe(() => {
      this.encodedServerInfo = encodeURIComponent(this.serverInfoService.getMarkdownFormatedServerInfo());
    });
  }

  ngOnChanges(): void {
    const uwuEnabledString = localStorage.getItem('uwu') ? 'uwu-' : '';
    this.frankframeworkLogoPath = `${this.IMAGES_BASE_PATH}${uwuEnabledString}frank-framework-logo.svg`;
    this.frankExclamationPath = `${this.IMAGES_BASE_PATH}${uwuEnabledString}frank-exclamation.svg`;
  }

  openInfo(): void {
    this.shouldOpenInfo.emit();
  }

  getClassByRoute(className: string, routeState: string | string[]): Record<string, boolean> {
    if (Array.isArray(routeState)) {
      return {
        [className]: routeState.some((routePartial) => this.router.url.split('?')[0].includes(routePartial)),
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
