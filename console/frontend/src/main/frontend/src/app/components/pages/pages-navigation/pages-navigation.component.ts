import { AfterViewInit, Component, EventEmitter, Input, OnChanges, OnInit, Output } from '@angular/core';
import { convertToParamMap, Router, RouterModule } from '@angular/router';
import { MinimalizaSidebarComponent } from './minimaliza-sidebar.component';
import { ScrollToTopComponent } from './scroll-to-top.component';
import { CommonModule } from '@angular/common';
import { CustomViewsComponent } from '../../custom-views/custom-views.component';
import { InformationModalComponent } from '../information-modal/information-modal.component';
import { ServerInfoService } from '../../../services/server-info.service';
import { CdkAccordionItem, CdkAccordionModule } from '@angular/cdk/accordion';
import { SidebarDirective } from './sidebar.directive';
import { HasAccessToLinkDirective } from '../../has-access-to-link.directive';

type ExpandedItem = {
  element: HTMLElement;
  accordionItem: CdkAccordionItem;
};

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
    HasAccessToLinkDirective,
  ],
})
export class PagesNavigationComponent implements OnChanges, OnInit, AfterViewInit {
  @Input() queryParams = convertToParamMap({});
  @Output() shouldOpenInfo = new EventEmitter<void>();

  protected frankframeworkLogoPath: string = 'assets/images/ff-kawaii.svg';
  protected frankExclamationPath: string = 'assets/images/frank-exclemation.svg';
  protected encodedServerInfo: string = '';

  private readonly IMAGES_BASE_PATH = 'assets/images/';
  private readonly ANIMATION_SPEED = 250;

  private expandedItem: ExpandedItem | null = null;
  private initializing: boolean = true;

  constructor(
    private router: Router,
    private serverInfoService: ServerInfoService,
  ) {}

  ngOnInit(): void {
    this.updateServerInfo();
    this.serverInfoService.serverInfo$.subscribe(() => {
      this.updateServerInfo();
    });
  }

  ngOnChanges(): void {
    const uwuEnabledString = localStorage.getItem('uwu') ? 'uwu-' : '';
    this.frankframeworkLogoPath = `${this.IMAGES_BASE_PATH}${uwuEnabledString}frank-framework-logo.svg`;
    this.frankExclamationPath = `${this.IMAGES_BASE_PATH}${uwuEnabledString}frank-exclamation.svg`;
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.initializing = false;
    });
  }

  openInfo(): void {
    this.shouldOpenInfo.emit();
  }

  getClassByRoute(className: string, routeState: string | string[], isExpanded?: boolean): Record<string, boolean> {
    return {
      [className]: isExpanded || this.getExpandedByRoute(routeState),
    };
  }

  getExpandedByRoute(routeState: string | string[], templateInfo?: ExpandedItem): boolean {
    const expanded = Array.isArray(routeState)
      ? routeState.some((routePartial) => this.router.url.split('?')[0].includes(routePartial))
      : this.router.url.split('?')[0].includes(routeState);
    if (this.initializing && expanded && templateInfo) {
      this.expandedItem = templateInfo;
    }
    return expanded;
  }

  getConfigurationsQueryParam(): string | null {
    return this.queryParams.get('configuration');
  }

  collapseItem(element: HTMLElement, accordionItem: CdkAccordionItem): void {
    element.style.opacity = '1';
    element.style.height = `${element.clientHeight}px`;
    accordionItem.toggle();
    element.animate({ opacity: 0, height: `0px` }, this.ANIMATION_SPEED).finished.then(() => {
      element.removeAttribute('style');
    });
  }

  expandCollapse(accordionItem: CdkAccordionItem, event: MouseEvent): void {
    const toBeExpanded = !accordionItem.expanded;
    const eventElement = event.target as HTMLElement;
    const parentElement =
      eventElement.tagName === 'A' ? eventElement.parentElement! : eventElement.parentElement!.parentElement!;
    const element = parentElement.children[1] as HTMLElement;

    if (toBeExpanded) {
      if (this.expandedItem) {
        this.collapseItem(this.expandedItem.element, this.expandedItem.accordionItem);
      }
      const height = [...element.children].reduce((total, child) => total + child.clientHeight, 0);
      element.style.opacity = '0';
      element.style.height = '0';
      accordionItem.toggle();
      element.animate({ opacity: 1, height: `${height}px` }, this.ANIMATION_SPEED).finished.then(() => {
        element.removeAttribute('style');
      });
      this.expandedItem = { element, accordionItem };
      return;
    }
    this.collapseItem(element, accordionItem);
    this.expandedItem = null;
  }

  closeExpandedItem(): void {
    if (this.expandedItem) {
      this.collapseItem(this.expandedItem.element, this.expandedItem.accordionItem);
      this.expandedItem = null;
    }
  }

  private updateServerInfo(): void {
    this.encodedServerInfo = encodeURIComponent(this.serverInfoService.getMarkdownFormatedServerInfo());
  }
}
