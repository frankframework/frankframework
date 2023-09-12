import { DoBootstrap, InjectionToken, NgModule, ValueProvider } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { UpgradeModule, downgradeComponent } from '@angular/upgrade/static';
import { HttpClientModule } from '@angular/common/http';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { $stateServiceProvider } from './ajs-deps-services';
import { UIRouterUpgradeModule } from '@uirouter/angular-hybrid';
import { NgIdleModule } from '@ng-idle/core';

import { AppRoutingModule } from './app-routing.module';

import '../angularjs/main';
import '../angularjs/app/app.module';
import '../angularjs/app/app.config';
import '../angularjs/services';
import '../angularjs/filters';
import '../angularjs/directives';
import '../angularjs/controllers';
import '../angularjs/components';

import { PagesFooterComponent } from './components/pages/pages-footer/pages-footer.component';
import { PagesNavigationComponent } from './components/pages/pages-navigation/pages-navigation.component';
import { sidebarServiceProvider } from './components/pages/ajs-sidebar-upgraded';
import { appServiceProvider } from './ajs-appservice-upgraded';
import { ScrollToTopComponent } from './components/pages/pages-navigation/scroll-to-top.component';
import { MinimalizaSidebarComponent } from './components/pages/pages-navigation/minimaliza-sidebar.component';
import { CustomViewsComponent } from './components/custom-views/custom-views.component';
import { PagesTopinfobarComponent } from './components/pages/pages-topinfobar/pages-topinfobar.component';
import { PagesTopnavbarComponent } from './components/pages/pages-topnavbar/pages-topnavbar.component';
import { HamburgerComponent } from './components/pages/pages-topnavbar/hamburger.component';
import { InputFileUploadComponent } from './components/input-file-upload/input-file-upload.component';
import { InlinestoreComponent } from './views/inlinestore/inlinestore.component';
import { JdbcBrowseTablesComponent } from './views/jdbc/jdbc-browse-tables/jdbc-browse-tables.component';
import { OrderByPipe } from './pipes/orderby.pipe';
import { FormsModule } from '@angular/forms';
import { JdbcExecuteQueryComponent } from './views/jdbc/jdbc-execute-query/jdbc-execute-query.component';
import { IframeCustomViewComponent } from './views/iframe/iframe-custom-view/iframe-custom-view.component';
import { LaddaModule } from 'angular2-ladda';
import { IframeLadybugComponent } from './views/iframe/iframe-ladybug/iframe-ladybug.component';
import { IframeLadybugBetaComponent } from './views/iframe/iframe-ladybug-beta/iframe-ladybug-beta.component';
import { IframeLarvaComponent } from './views/iframe/iframe-larva/iframe-larva.component';
import { IbisstoreSummaryComponent } from './views/ibisstore-summary/ibisstore-summary.component';
import { StatusComponent } from './views/status/status.component';
import { LogoutComponent } from './components/logout/logout.component';
import { ConfigurationFilterPipe } from './pipes/configuration-filter.pipe';
import { SearchFilterPipe } from './pipes/search-filter.pipe';
import { TruncatePipe } from './pipes/truncate.pipe';
import { ToDateDirective } from './components/to-date.directive';

import {
  alertServiceProvider,
  apiServiceProvider,
  authServiceProvider,
  base64ServiceProvider,
  cookiesServiceProvider,
  debugServiceProvider,
  gdprServiceProvider,
  miscServiceProvider,
  notificationServiceProvider,
  pollerServiceProvider,
  sessionServiceProvider,
  sweetalertServiceProvider,
  toastrServiceProvider
} from './ajs-upgraded-services';
import { AppConstants, appConstants, appModule } from '../angularjs/app/app.module';
import { TimeSinceDirective } from './components/time-since.directive';
import { FlowComponent } from './views/status/flow/flow.component';

export const APPCONSTANTS = new InjectionToken<AppConstants>('app.appConstants');

const appConstantsProvider: ValueProvider = {
  provide: APPCONSTANTS,
  useValue: appConstants
}
const windowProvider: ValueProvider = {
  provide: Window,
  useValue: window
}

appModule
  .directive('hamburger', downgradeComponent({ component: HamburgerComponent }) as angular.IDirectiveFactory)
  .directive('minimalizaSidebar', downgradeComponent({ component: MinimalizaSidebarComponent }) as angular.IDirectiveFactory)
  .directive('pagesFooter', downgradeComponent({ component: PagesFooterComponent }) as angular.IDirectiveFactory)
  .directive('pagesNavigation', downgradeComponent({ component: PagesNavigationComponent }) as angular.IDirectiveFactory)
  .directive('pagesTopinfobar', downgradeComponent({ component: PagesTopinfobarComponent }) as angular.IDirectiveFactory)
  .directive('pagesTopnavbar', downgradeComponent({ component: PagesTopnavbarComponent }) as angular.IDirectiveFactory)
  .directive('scrollToTop', downgradeComponent({ component: ScrollToTopComponent }) as angular.IDirectiveFactory)
  .directive('inputFileUpload', downgradeComponent({ component: InputFileUploadComponent }) as angular.IDirectiveFactory)
  .directive('logout', downgradeComponent({ component: LogoutComponent }) as angular.IDirectiveFactory)
  .directive('inlineStore', downgradeComponent({ component: InlinestoreComponent }) as angular.IDirectiveFactory)
  .directive('jdbcBrowseTables', downgradeComponent({ component: JdbcBrowseTablesComponent }) as angular.IDirectiveFactory)
  .directive('jdbcExecuteQuery', downgradeComponent({ component: JdbcExecuteQueryComponent }) as angular.IDirectiveFactory)
  .directive('iframeCustomView', downgradeComponent({ component: IframeCustomViewComponent }) as angular.IDirectiveFactory)
  .directive('iframeLadybug', downgradeComponent({ component: IframeLadybugComponent }) as angular.IDirectiveFactory)
  .directive('iframeLadybugBeta', downgradeComponent({ component: IframeLadybugBetaComponent }) as angular.IDirectiveFactory)
  .directive('iframeLarva', downgradeComponent({ component: IframeLarvaComponent }) as angular.IDirectiveFactory)
  .directive('ibisstoreSummary', downgradeComponent({ component: IbisstoreSummaryComponent }) as angular.IDirectiveFactory)
  .directive('status', downgradeComponent({ component: StatusComponent }) as angular.IDirectiveFactory)
  .directive('flow', downgradeComponent({ component: FlowComponent }) as angular.IDirectiveFactory);


@NgModule({
  declarations: [
    PagesFooterComponent,
    PagesNavigationComponent,
    ScrollToTopComponent,
    MinimalizaSidebarComponent,
    CustomViewsComponent,
    PagesTopinfobarComponent,
    PagesTopnavbarComponent,
    HamburgerComponent,
    InputFileUploadComponent,
    LogoutComponent,
    InlinestoreComponent,
    JdbcBrowseTablesComponent,
    JdbcExecuteQueryComponent,
    IframeCustomViewComponent,
    IframeLadybugComponent,
    IframeLadybugBetaComponent,
    IframeLarvaComponent,
    IbisstoreSummaryComponent,
    StatusComponent,

    // pipes
    OrderByPipe,
    ConfigurationFilterPipe,
    SearchFilterPipe,
    TruncatePipe,

    // directives
    ToDateDirective,
    TimeSinceDirective,
    FlowComponent
  ],
  imports: [
    BrowserModule,
    UpgradeModule,
    FormsModule,
    HttpClientModule,
    LaddaModule,
    NgbModule,
    AppRoutingModule,
    UIRouterUpgradeModule.forRoot(),
    NgIdleModule.forRoot(),
  ],
  providers: [
    alertServiceProvider,
    apiServiceProvider,
    authServiceProvider,
    base64ServiceProvider,
    cookiesServiceProvider,
    debugServiceProvider,
    gdprServiceProvider,
    miscServiceProvider,
    notificationServiceProvider,
    pollerServiceProvider,
    sessionServiceProvider,
    sweetalertServiceProvider,
    toastrServiceProvider,
    appConstantsProvider,
    windowProvider,

    // deps
    $stateServiceProvider,

    // scoped services
    appServiceProvider,
    sidebarServiceProvider,
  ]
})

export class AppModule implements DoBootstrap {
  constructor(private upgrade: UpgradeModule) { }
  ngDoBootstrap() {
    this.upgrade.bootstrap(document.documentElement, ['iaf.beheerconsole']);
    // this.upgrade.bootstrap(document.body, ['iaf.beheerconsole'], { strictDi: true });
  }
}
