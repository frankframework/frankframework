import { NgModule, ValueProvider } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { HttpClientModule, HttpClientXsrfModule } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { TitleStrategy } from '@angular/router';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { NgIdleModule } from '@ng-idle/core';
import { NgChartsModule } from 'ng2-charts';
import { LaddaModule } from 'angular2-ladda';

import { AppRoutingModule } from './app-routing.module';

import { AppComponent } from './app.component';
import { PagesFooterComponent } from './components/pages/pages-footer/pages-footer.component';
import { PagesNavigationComponent } from './components/pages/pages-navigation/pages-navigation.component';
import { CustomViewsComponent } from './components/custom-views/custom-views.component';
import { PagesTopinfobarComponent } from './components/pages/pages-topinfobar/pages-topinfobar.component';
import { PagesTopnavbarComponent } from './components/pages/pages-topnavbar/pages-topnavbar.component';
import { InputFileUploadComponent } from './components/input-file-upload/input-file-upload.component';
import { InlinestoreComponent } from './views/inlinestore/inlinestore.component';
import { JdbcBrowseTablesComponent } from './views/jdbc/jdbc-browse-tables/jdbc-browse-tables.component';
import { OrderByPipe } from './pipes/orderby.pipe';
import { JdbcExecuteQueryComponent } from './views/jdbc/jdbc-execute-query/jdbc-execute-query.component';
import { IframeCustomViewComponent } from './views/iframe/iframe-custom-view/iframe-custom-view.component';
import { IframeLadybugLegacyComponent } from './views/iframe/iframe-ladybug-legacy/iframe-ladybug-legacy.component';
import { IframeLadybugComponent } from './views/iframe/iframe-ladybug/iframe-ladybug.component';
import { IframeLarvaComponent } from './views/iframe/iframe-larva/iframe-larva.component';
import { IbisstoreSummaryComponent } from './views/ibisstore-summary/ibisstore-summary.component';
import { StatusComponent } from './views/status/status.component';
import { LogoutComponent } from './components/logout/logout.component';
import { ConfigurationFilterPipe } from './pipes/configuration-filter.pipe';
import { SearchFilterPipe } from './pipes/search-filter.pipe';
import { TruncatePipe } from './pipes/truncate.pipe';
import { ToDateDirective } from './components/to-date.directive';
import { LiquibaseComponent } from './views/liquibase/liquibase.component';
import { JmsSendMessageComponent } from './views/jms/jms-send-message/jms-send-message.component';
import { JmsBrowseQueueComponent } from './views/jms/jms-browse-queue/jms-browse-queue.component';
import { EnvironmentVariablesComponent } from './views/environment-variables/environment-variables.component';
import { VariablesFilterPipe } from './pipes/variables-filter.pipe';
import { TimeSinceDirective } from './components/time-since.directive';
import { FlowComponent } from './views/status/flow/flow.component';
import { StorageComponent } from './views/storage/storage.component';
import { StorageListComponent } from './views/storage/storage-list/storage-list.component';
import { StorageViewComponent } from './views/storage/storage-view/storage-view.component';
import { StorageListDtComponent } from './views/storage/storage-list/storage-list-dt/storage-list-dt.component';
import { AdapterstatisticsComponent } from './views/adapterstatistics/adapterstatistics.component';
import { FormatStatisticsPipe } from './views/adapterstatistics/format-statistics.pipe';
import { DropLastCharPipe } from './pipes/drop-last-char.pipe';
import { QuickSubmitFormDirective } from './components/quick-submit-form.directive';
import { FormatStatKeysPipe } from './views/adapterstatistics/format-stat-keys.pipe';
import { SecurityItemsComponent } from './views/security-items/security-items.component';
import { WebservicesComponent } from './views/webservices/webservices.component';
import { SchedulerComponent } from './views/scheduler/scheduler.component';
import { SchedulerEditComponent } from './views/scheduler/scheduler-edit/scheduler-edit.component';
import { SchedulerAddComponent } from './views/scheduler/scheduler-add/scheduler-add.component';
import { ConfigurationsManageComponent } from './views/configurations/configurations-manage/configurations-manage.component';
import { ConfigurationsShowComponent } from './views/configurations/configurations-show/configurations-show.component';
import { ConfigurationsUploadComponent } from './views/configurations/configurations-upload/configurations-upload.component';
import { ConfigurationsManageDetailsComponent } from './views/configurations/configurations-manage/configurations-manage-details/configurations-manage-details.component';
import { MonitorsComponent } from './views/monitors/monitors.component';
import { MonitorsAddEditComponent } from './views/monitors/monitors-add-edit/monitors-add-edit.component';
import { PagesTitleStrategy } from './pages-title-strategy';
import { WithJavaListenerPipe } from './pipes/with-java-listener.pipe';
import { InformationModalComponent } from './components/pages/information-modal/information-modal.component';
import { FlowModalComponent } from './views/status/flow/flow-modal/flow-modal.component';
import { NgMermaidComponent } from './components/ng-mermaid/ng-mermaid.component';
import { LoggingComponent } from './views/logging/logging.component';
import { LoggingManageComponent } from './views/logging/logging-manage/logging-manage.component';
import { ConnectionsComponent } from './views/connections/connections.component';
import { ErrorComponent } from './views/error/error.component';
import { LoadingComponent } from './views/loading/loading.component';
import { NotificationsComponent } from './views/notifications/notifications.component';
import { IafUpdateComponent } from './views/iaf-update/iaf-update.component';
import { MarkDownPipe } from './pipes/mark-down.pipe';
import { TestPipelineComponent } from './views/test-pipeline/test-pipeline.component';
import { TestServiceListenerComponent } from './views/test-service-listener/test-service-listener.component';
import { LoginComponent } from './views/login/login.component';
import { httpInterceptorProviders } from './http-interceptors';
import { ToastsContainerComponent } from './components/toasts-container/toasts-container.component';
import { ThSortableDirective } from './components/th-sortable.directive';
import { FileViewerComponent } from './components/file-viewer/file-viewer.component';
import { HumanFileSizePipe } from './pipes/human-file-size.pipe';
import { MonacoEditorComponent } from './components/monaco-editor/monaco-editor.component';
import { ServerWarningsComponent } from './views/status/server-warnings/server-warnings.component';
import { AdapterStatusComponent } from './views/status/adapter-status/adapter-status.component';
import { ConfigurationMessagesComponent } from './views/status/configuration-messages/configuration-messages.component';
import { ConfigurationSummaryComponent } from './views/status/configuration-summary/configuration-summary.component';
import { LoggingAddComponent } from './views/logging/logging-add/logging-add.component';
import { DatatableComponent } from './components/datatable/datatable.component';
import { DtContentDirective } from './components/datatable/dt-content.directive';
import { MonitorsNewComponent } from './views/monitors/monitors-new/monitors-new.component';
import { ConfigurationTabListComponent } from './components/tab-list/configuration-tab-list.component';
import { TabListComponent } from './components/tab-list/tab-list.component';

const windowProvider: ValueProvider = {
  provide: Window,
  useValue: window,
};

@NgModule({
  declarations: [
    AppComponent,
    ConfigurationsManageComponent,
    ConfigurationsManageDetailsComponent,
    ConfigurationsShowComponent,
    ConfigurationsUploadComponent,
    EnvironmentVariablesComponent,
    FlowComponent,
    IbisstoreSummaryComponent,
    IframeCustomViewComponent,
    IframeLadybugLegacyComponent,
    IframeLadybugComponent,
    IframeLarvaComponent,
    InlinestoreComponent,
    JdbcBrowseTablesComponent,
    JdbcExecuteQueryComponent,
    JmsBrowseQueueComponent,
    JmsSendMessageComponent,
    LiquibaseComponent,
    SchedulerComponent,
    SchedulerEditComponent,
    SchedulerAddComponent,
    StatusComponent,
    StorageComponent,
    StorageListComponent,
    StorageViewComponent,
    StorageListDtComponent,
    AdapterstatisticsComponent,
    SecurityItemsComponent,
    WebservicesComponent,
    MonitorsComponent,
    MonitorsAddEditComponent,
    ConnectionsComponent,
    ErrorComponent,
    LoadingComponent,
    LoggingComponent,
    LoggingManageComponent,
    NotificationsComponent,
    IafUpdateComponent,
    TestPipelineComponent,
    TestServiceListenerComponent,
    LoginComponent,
    ServerWarningsComponent,
    AdapterStatusComponent,
    LoggingAddComponent,

    //modals
    FlowModalComponent,

    // pipes
    ConfigurationFilterPipe,
    DropLastCharPipe,
    OrderByPipe,
    SearchFilterPipe,
    VariablesFilterPipe,
    FormatStatisticsPipe,
    FormatStatKeysPipe,
    WithJavaListenerPipe,
    MarkDownPipe,

    // directives
    ConfigurationMessagesComponent,
    ConfigurationSummaryComponent,
  ],
  imports: [
    BrowserModule,
    FormsModule,
    HttpClientModule,
    HttpClientXsrfModule.withOptions(),
    LaddaModule,
    NgbModule,
    AppRoutingModule,
    NgIdleModule.forRoot(),
    NgChartsModule.forRoot(),

    // standalone directives
    TimeSinceDirective,
    ToDateDirective,
    ThSortableDirective,
    QuickSubmitFormDirective,

    // standalone components
    CustomViewsComponent,
    FileViewerComponent,
    InputFileUploadComponent,
    LogoutComponent,
    NgMermaidComponent,
    ToastsContainerComponent,
    InformationModalComponent,
    PagesFooterComponent,
    PagesNavigationComponent,
    PagesTopinfobarComponent,
    PagesTopnavbarComponent,
    HumanFileSizePipe,
    MonacoEditorComponent,
    DatatableComponent,
    DtContentDirective,
    MonitorsNewComponent,

    // standalone pipes
    TruncatePipe,
    ConfigurationTabListComponent,
    TabListComponent,
  ],
  providers: [windowProvider, { provide: TitleStrategy, useClass: PagesTitleStrategy }, httpInterceptorProviders],
  bootstrap: [AppComponent],
  exports: [TruncatePipe],
})
export class AppModule {}
