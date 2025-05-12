import {
  AfterViewInit,
  Component,
  EventEmitter,
  inject,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  Output,
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
import { AppConstants, AppService } from '../../../app.service';
import { Subscription } from 'rxjs';

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
  ],
})
export class PagesNavigationComponent implements OnChanges, OnInit, AfterViewInit, OnDestroy {
  @Input() queryParams = convertToParamMap({});
  @Output() shouldOpenInfo = new EventEmitter<void>();

  protected frankframeworkLogoPath: string = 'assets/images/ff-kawaii.svg';
  protected frankExclamationPath: string = 'assets/images/frank-exclemation.svg';
  protected showOldLadybug: boolean = false;
  protected encodedServerInfo: string = '';

  private readonly IMAGES_BASE_PATH = 'assets/images/';
  private readonly ANIMATION_SPEED = 250;

  private _subscriptions = new Subscription();
  private router: Router = inject(Router);
  private serverInfoService: ServerInfoService = inject(ServerInfoService);
  private readonly appService: AppService = inject(AppService);
  private appConstants: AppConstants = this.appService.APP_CONSTANTS;
  private expandedItem: ExpandedItem | null = null;
  private initializing: boolean = true;

  ngOnInit(): void {
    this.updateServerInfo();
    this.serverInfoService.serverInfo$.subscribe(() => {
      this.updateServerInfo();
    });
    const appConstantsSubscription = this.appService.appConstants$.subscribe(() => {
      this.appConstants = this.appService.APP_CONSTANTS;
      this.showOldLadybug = this.appConstants['testtool.echo2.enabled'] === 'true';
    });
    this._subscriptions.add(appConstantsSubscription);
    this.showOldLadybug = this.appConstants['testtool.echo2.enabled'] === 'true';
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

  ngOnDestroy(): void {
    this._subscriptions.unsubscribe();
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
