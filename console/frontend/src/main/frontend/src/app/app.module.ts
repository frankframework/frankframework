import { DoBootstrap, InjectionToken, NgModule, ValueProvider } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { HttpClientModule } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
// import { UpgradeModule, downgradeComponent } from '@angular/upgrade/static';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
// import { NgHybridStateDeclaration, UIRouterUpgradeModule } from '@uirouter/angular-hybrid';
import { NgIdleModule } from '@ng-idle/core';
import { NgChartsModule } from 'ng2-charts';
import { LaddaModule } from 'angular2-ladda';
import { DataTablesModule } from 'angular-datatables';

import { AppRoutingModule } from './app-routing.module';

import '../angularjs/main';
// import '../angularjs/app/app.module';
// import '../angularjs/app/app.config';
// import '../angularjs/services';
// import '../angularjs/filters';
// import '../angularjs/directives';
// import '../angularjs/controllers';
// import '../angularjs/components';

import { AppComponent } from './app.component';
import { PagesFooterComponent } from './components/pages/pages-footer/pages-footer.component';
import { PagesNavigationComponent } from './components/pages/pages-navigation/pages-navigation.component';
import { ScrollToTopComponent } from './components/pages/pages-navigation/scroll-to-top.component';
import { MinimalizaSidebarComponent } from './components/pages/pages-navigation/minimaliza-sidebar.component';
import { CustomViewsComponent } from './components/custom-views/custom-views.component';
import { PagesTopinfobarComponent } from './components/pages/pages-topinfobar/pages-topinfobar.component';
import { PagesTopnavbarComponent } from './components/pages/pages-topnavbar/pages-topnavbar.component';
import { HamburgerComponent } from './components/pages/pages-topnavbar/hamburger.component';
import { InputFileUploadComponent } from './components/input-file-upload/input-file-upload.component';
// import { InlinestoreComponent } from './views/inlinestore/inlinestore.component';
import { JdbcBrowseTablesComponent } from './views/jdbc/jdbc-browse-tables/jdbc-browse-tables.component';
import { OrderByPipe } from './pipes/orderby.pipe';
import { JdbcExecuteQueryComponent } from './views/jdbc/jdbc-execute-query/jdbc-execute-query.component';
import { IframeCustomViewComponent } from './views/iframe/iframe-custom-view/iframe-custom-view.component';
import { IframeLadybugComponent } from './views/iframe/iframe-ladybug/iframe-ladybug.component';
import { IframeLadybugBetaComponent } from './views/iframe/iframe-ladybug-beta/iframe-ladybug-beta.component';
import { IframeLarvaComponent } from './views/iframe/iframe-larva/iframe-larva.component';
// import { IbisstoreSummaryComponent } from './views/ibisstore-summary/ibisstore-summary.component';
import { StatusComponent } from './views/status/status.component';
import { LogoutComponent } from './components/logout/logout.component';
import { ConfigurationFilterPipe } from './pipes/configuration-filter.pipe';
import { SearchFilterPipe } from './pipes/search-filter.pipe';
import { TruncatePipe } from './pipes/truncate.pipe';
import { ToDateDirective } from './components/to-date.directive';
import { LiquibaseComponent } from './views/liquibase/liquibase.component';
import { JmsSendMessageComponent } from './views/jms/jms-send-message/jms-send-message.component';
import { JmsBrowseQueueComponent } from './views/jms/jms-browse-queue/jms-browse-queue.component';
// import { EnvironmentVariablesComponent } from './views/environment-variables/environment-variables.component';
// import { VariablesFilterPipe } from './pipes/variablesFilter.pipe';
import { TimeSinceDirective } from './components/time-since.directive';
import { FlowComponent } from './views/status/flow/flow.component';
import { StorageComponent } from './views/storage/storage.component';
import { StorageListComponent } from './views/storage/storage-list/storage-list.component';
import { StorageViewComponent } from './views/storage/storage-view/storage-view.component';
import { StorageListDtComponent } from './views/storage/storage-list/storage-list-dt/storage-list-dt.component';
// import { AdapterstatisticsComponent } from './views/adapterstatistics/adapterstatistics.component';
// import { FormatStatisticsPipe } from './views/adapterstatistics/format-statistics.pipe';
import { DropLastCharPipe } from './pipes/drop-last-char.pipe';
import { QuickSubmitFormDirective } from './views/jdbc/jdbc-execute-query/quick-submit-form.directive';
// import { FormatStatKeysPipe } from './views/adapterstatistics/format-stat-keys.pipe';
import { FitHeightDirective } from './views/iframe/fit-height.directive';
// import { SecurityItemsComponent } from './views/security-items/security-items.component';
// import { WebservicesComponent } from './views/webservices/webservices.component';
import { SideNavigationDirective } from './components/pages/side-navigation.directive';

const windowProvider: ValueProvider = {
  provide: Window,
  useValue: window
}


@NgModule({
  declarations: [
    AppComponent,
    CustomViewsComponent,
    // EnvironmentVariablesComponent,
    FlowComponent,
    HamburgerComponent,
    // IbisstoreSummaryComponent,
    IframeCustomViewComponent,
    IframeLadybugComponent,
    IframeLadybugBetaComponent,
    IframeLarvaComponent,
    // InlinestoreComponent,
    InputFileUploadComponent,
    JdbcBrowseTablesComponent,
    JdbcExecuteQueryComponent,
    JmsBrowseQueueComponent,
    JmsSendMessageComponent,
    LiquibaseComponent,
    LogoutComponent,
    MinimalizaSidebarComponent,
    PagesFooterComponent,
    PagesNavigationComponent,
    PagesTopinfobarComponent,
    PagesTopnavbarComponent,
    ScrollToTopComponent,
    StatusComponent,
    StorageComponent,
    StorageListComponent,
    StorageViewComponent,
    StorageListDtComponent,
    // AdapterstatisticsComponent,
    // SecurityItemsComponent,
    // WebservicesComponent,

    // pipes
    ConfigurationFilterPipe,
    DropLastCharPipe,
    OrderByPipe,
    SearchFilterPipe,
    TruncatePipe,
    // VariablesFilterPipe,
    // FormatStatisticsPipe,
    // FormatStatKeysPipe,

    // directives
    ToDateDirective,
    TimeSinceDirective,
    QuickSubmitFormDirective,
    FitHeightDirective,
    SideNavigationDirective,
  ],
  imports: [
    BrowserModule,
    FormsModule,
    HttpClientModule,
    LaddaModule,
    NgbModule,
    // UpgradeModule,
    AppRoutingModule,
    // UIRouterUpgradeModule.forRoot(/* { states: nestedRouterStates } */),
    NgIdleModule.forRoot(),
    NgChartsModule.forRoot(),
    DataTablesModule
  ],
  providers: [
    windowProvider
  ],
  bootstrap: [AppComponent]
})
export class AppModule {}
