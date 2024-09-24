import { Component, EventEmitter, Input, OnChanges, OnInit, Output } from '@angular/core';
import { convertToParamMap, Router, RouterModule } from '@angular/router';
import { MinimalizaSidebarComponent } from './minimaliza-sidebar.component';
import { ScrollToTopComponent } from './scroll-to-top.component';
import { CommonModule } from '@angular/common';
import { CustomViewsComponent } from '../../custom-views/custom-views.component';
import { InformationModalComponent } from '../information-modal/information-modal.component';
import { ServerInfoService } from '../../../services/server-info.service';
import { CdkAccordionItem, CdkAccordionModule } from '@angular/cdk/accordion';
import { SidebarDirective } from './sidebar.directive';

@Component({
  selector: 'app-pages-navigation',
  templateUrl: './pages-navigation.component.html',
  styleUrls: ['./pages-navigation.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    CdkAccordionModule,
    CustomViewsComponent,
    MinimalizaSidebarComponent,
    ScrollToTopComponent,
    InformationModalComponent,
    SidebarDirective,
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

  getClassByRoute(className: string, routeState: string | string[], isExpanded?: boolean): Record<string, boolean> {
    return {
      [className]: isExpanded || this.getExpandedByRoute(routeState),
    };
  }

  getExpandedByRoute(routeState: string | string[]): boolean {
    if (Array.isArray(routeState)) {
      return routeState.some((routePartial) => this.router.url.split('?')[0].includes(routePartial));
    }
    return this.router.url.split('?')[0].includes(routeState);
  }

  getConfigurationsQueryParam(): string | null {
    return this.queryParams.get('configuration');
  }

  expandCollapse(accordionItem: CdkAccordionItem, event: MouseEvent): void {
    const toBeExpanded = !accordionItem.expanded;
    const eventElement = event.target as HTMLElement;
    const parentElement =
      eventElement.tagName === 'A' ? eventElement.parentElement! : eventElement.parentElement!.parentElement!;
    const element = parentElement.children[1] as HTMLElement;
    const height = [...element.children].reduce((total, child) => total + child.clientHeight, 0);

    if (toBeExpanded) {
      element.style.opacity = '0';
      element.style.height = '0';
      accordionItem.toggle();
      element.animate({ opacity: 1, height: `${height}px` }, 150).finished.then(() => {
        element.removeAttribute('style');
      });
    } else {
      element.style.opacity = '1';
      element.style.height = `${height}px`;
      accordionItem.toggle();
      element.animate({ opacity: 0, height: `0px` }, 150).finished.then(() => {
        element.removeAttribute('style');
      });
    }
  }
}
