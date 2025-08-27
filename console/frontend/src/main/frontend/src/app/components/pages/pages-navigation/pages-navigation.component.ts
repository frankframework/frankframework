import {
  AfterViewInit,
  Component,
  computed,
  EventEmitter,
  inject,
  Input,
  OnChanges,
  Output,
  Signal,
} from '@angular/core';
import { convertToParamMap, Router, RouterModule } from '@angular/router';
import { MinimalizaSidebarComponent } from './minimaliza-sidebar.component';
import { ScrollToTopComponent } from './scroll-to-top.component';
import { CustomViewsComponent } from '../../custom-views/custom-views.component';
import { ServerInfoService } from '../../../services/server-info.service';
import { CdkAccordionItem, CdkAccordionModule } from '@angular/cdk/accordion';
import { SidebarDirective } from './sidebar.directive';
import { HasAccessToLinkDirective } from '../../has-access-to-link.directive';
import { NgClass } from '@angular/common';
import { AppService } from '../../../app.service';
import { ConditionalOnPropertyDirective } from '../../conditional-on-property.directive';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import {
  faAngleDown,
  faBook,
  faBug,
  faCogs,
  faCubes,
  faDashboard,
  faInfoCircle,
  faLanguage,
  faLock,
  faSitemap,
  faThLarge,
  faLifeRing,
} from '@fortawesome/free-solid-svg-icons';
import { faCalendar, faClone, faCommenting, faFlag, faPaperPlane } from '@fortawesome/free-regular-svg-icons';

type ExpandedItem = {
  element: HTMLElement;
  accordionItem: CdkAccordionItem;
};

@Component({
  selector: 'app-pages-navigation',
  templateUrl: './pages-navigation.component.html',
  styleUrls: ['./pages-navigation.component.scss'],
  imports: [
    RouterModule,
    CdkAccordionModule,
    CustomViewsComponent,
    MinimalizaSidebarComponent,
    ScrollToTopComponent,
    SidebarDirective,
    HasAccessToLinkDirective,
    NgClass,
    ConditionalOnPropertyDirective,
    FaIconComponent,
  ],
})
export class PagesNavigationComponent implements OnChanges, AfterViewInit {
  @Input() queryParams = convertToParamMap({});
  @Output() shouldOpenInfo = new EventEmitter<void>();

  protected readonly faDashboard = faDashboard;
  protected readonly faCogs = faCogs;
  protected readonly faLifeRing = faLifeRing;
  protected readonly faBug = faBug;
  protected readonly faCubes = faCubes;
  protected readonly faThLarge = faThLarge;
  protected readonly faPaperPlane = faPaperPlane;
  protected readonly faCalendar = faCalendar;
  protected readonly faClone = faClone;
  protected readonly faLanguage = faLanguage;
  protected readonly faSitemap = faSitemap;
  protected readonly faLock = faLock;
  protected readonly faInfoCircle = faInfoCircle;
  protected readonly faBook = faBook;
  protected readonly faCommenting = faCommenting;
  protected readonly faFlag = faFlag;
  protected frankframeworkLogoPath = 'assets/images/ff-kawaii.svg';
  protected frankExclamationPath = 'assets/images/frank-exclemation.svg';
  protected showOldLadybug: Signal<boolean> = computed(
    () => this.appService.appConstants()['testtool.echo2.enabled'] === 'true',
  );
  protected encodedServerInfo: Signal<string> = computed(() => {
    if (!this.serverInfoService.serverInfo()) return '';
    return encodeURIComponent(this.serverInfoService.getMarkdownFormatedServerInfo());
  });

  private readonly router: Router = inject(Router);
  private readonly serverInfoService: ServerInfoService = inject(ServerInfoService);
  private readonly appService: AppService = inject(AppService);
  private readonly IMAGES_BASE_PATH = 'assets/images/';
  private readonly ANIMATION_SPEED = 250;
  private expandedItem: ExpandedItem | null = null;
  private initializing = true;

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

  protected readonly faAngleDown = faAngleDown;
}
