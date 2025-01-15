import { Component, inject, Input, OnChanges, OnDestroy, OnInit } from '@angular/core';
import { TabListComponent } from './tab-list.component';
import { ActivatedRoute, Router } from '@angular/router';
import { AppService, Configuration } from '../../app.service';
import { NgClass } from '@angular/common';
import { first, Subscription } from 'rxjs';

@Component({
  selector: 'app-configuration-tab-list',
  imports: [NgClass],
  templateUrl: './tab-list.component.html',
  styleUrl: './tab-list.component.scss',
})
export class ConfigurationTabListComponent extends TabListComponent implements OnInit, OnChanges, OnDestroy {
  @Input() queryParamName: string = 'name';
  @Input() showAll: boolean = false;
  @Input() filterIAF_Util: boolean = false;

  private route: ActivatedRoute = inject(ActivatedRoute);
  private router: Router = inject(Router);
  private appService: AppService = inject(AppService);
  private configurationsList: string[] = [];
  private subscriptions: Subscription = new Subscription();

  @Input({ required: true })
  set configurations(configurations: Configuration[]) {
    const tabs = configurations.map((configuration) => configuration.name);
    this.tabs = tabs;
    this.configurationsList = tabs;
  }

  ngOnInit(): void {
    const adaptersSubscription = this.appService.adapters$.subscribe(() => {
      this.processTabList();
    });
    this.subscriptions.add(adaptersSubscription);

    this.route.queryParamMap.subscribe((parameters) => {
      const tab = parameters.get(this.queryParamName);
      if (tab == this.selectedTab) {
        return;
      } else if (tab) {
        this.setSelectedTab(tab);
      } else if (this.showAllTab) {
        this.setSelectedTab(this._allTabName);
      } else if (this.tabsList.length > 0) {
        this.setSelectedTab(this.tabsList[0]);
      }
    });
    this.appService.selectedConfigurationTab$.pipe(first()).subscribe((tab) => {
      if (tab !== null) this.changeTab(tab);
    });
  }

  ngOnChanges(): void {
    this.processTabList();
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  protected override changeTab(tab: string): void {
    this.appService.updateSelectedConfigurationTab(tab);
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: tab === this._allTabName ? { [this.queryParamName]: null } : { [this.queryParamName]: tab },
      queryParamsHandling: 'merge',
      skipLocationChange: true,
    });
  }

  private setSelectedTab(tab: string): void {
    this.selectedTab = tab;
    this.selectedTabChange.emit(tab);
  }

  private processTabList(): void {
    if (!this.showAll) {
      this.tabs = this.configurationsList.filter((configuration) => {
        if (configuration === 'IAF_Util' && this.filterIAF_Util) return false;
        return this.appService.configurationLengths[configuration] > 0;
      });
    }
  }
}
