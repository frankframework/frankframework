import { appModule } from './app/app.module';
import { downgradeComponent } from '@angular/upgrade/static';

import './app/app.component';

import './app/components/custom-views/custom-views.component';
import './app/components/input-file-upload/input-file-upload.component';
import './app/components/logout/logout.component';
import { PagesFooterComponent } from 'src/app/components/pages/pages-footer/pages-footer.component';
import { MinimalizaSidebarComponent } from 'src/app/components/pages/pages-navigation/minimaliza-sidebar.component';
import { PagesNavigationComponent } from 'src/app/components/pages/pages-navigation/pages-navigation.component';
import { ScrollToTopComponent } from 'src/app/components/pages/pages-navigation/scroll-to-top.component';
import { PagesTopinfobarComponent } from 'src/app/components/pages/pages-topinfobar/pages-topinfobar.component';
import { HamburgerComponent } from 'src/app/components/pages/pages-topnavbar/hamburger.component';
import { PagesTopnavbarComponent } from 'src/app/components/pages/pages-topnavbar/pages-topnavbar.component';

import './app/views/adapterstatistics/adapterstatistics.component';
import './app/views/configurations/configurations-manage/configurations-manage.component';
import './app/views/configurations/configurations-manage/configurations-manage-details/configurations-manage-details.component';
import './app/views/configurations/configurations-show/configurations-show.component';
import './app/views/configurations/configurations-upload/configurations-upload.component';
import './app/views/connections/connections.component';
import './app/views/environment-variables/environment-variables.component';
import './app/views/error/error.component';
import './app/views/iaf-update/iaf-update-status.component';
import './app/views/ibisstore-summary/ibisstore-summary.component';
import './app/views/iframe/iframe-custom-view/iframe-custom-view.component';
import './app/views/iframe/iframe-ladybug/iframe-ladybug.component';
import './app/views/iframe/iframe-ladybug-beta/iframe-ladybug-beta.component';
import './app/views/iframe/iframe-larva/iframe-larva.component';
import './app/views/inlinestore/inlinestore.component';
import './app/views/jdbc/jdbc-execute-query/jdbc-execute-query.component';
import './app/views/jdbc/jdbc-browse-tables/jdbc-browse-tables.component';
import './app/views/jdbc/jdbc-execute-query/jdbc-execute-query.component';
import './app/views/jms/jms-browse-queue/jms-browse-queue.component';
import './app/views/jms/jms-send-message/jms-send-message.component';
import './app/views/liquibase/liquibase.component';
import './app/views/loading/loading.component';
import './app/views/login/login.component';
import './app/views/logging/logging.component';
import './app/views/logging/logging-manage/logging-manage.component';
import './app/views/monitors/monitors-add-edit/monitors-add-edit.component';
import './app/views/monitors/monitors.component';
import './app/views/notifications/notifications.component';
import './app/views/scheduler/scheduler.component';
import './app/views/scheduler/scheduler-add/scheduler-add.component';
import './app/views/scheduler/scheduler-edit/scheduler-edit.component';
import './app/views/security-items/security-items.component';
import './app/views/status/status.component';
import './app/views/status/flow/flow.component';
import './app/views/status/flow/flow-modal/flow-modal.component';
import './app/views/storage/storage.component';
import './app/views/storage/storage-list/storage-list.component';
import './app/views/storage/storage-view/storage-view.component';
import './app/views/test-pipeline/test-pipeline.component';
import './app/views/test-service-listener/test-service-listener.component';
import './app/views/webservices/webservices.component';

// appModule
//   .directive('hamburger', downgradeComponent({ component: HamburgerComponent }) as angular.IDirectiveFactory)
//   .directive('minimalizaSidebar', downgradeComponent({ component: MinimalizaSidebarComponent }) as angular.IDirectiveFactory)
//   .directive('pagesFooter', downgradeComponent({ component: PagesFooterComponent }) as angular.IDirectiveFactory)
//   .directive('pagesNavigation', downgradeComponent({ component: PagesNavigationComponent }) as angular.IDirectiveFactory)
//   .directive('pagesTopinfobar', downgradeComponent({ component: PagesTopinfobarComponent }) as angular.IDirectiveFactory)
//   .directive('pagesTopnavbar', downgradeComponent({ component: PagesTopnavbarComponent }) as angular.IDirectiveFactory)
//   .directive('scrollToTop', downgradeComponent({ component: ScrollToTopComponent }) as angular.IDirectiveFactory);
