import { Component, inject, Input, OnInit } from '@angular/core';
import { TabListComponent } from './tab-list.component';
import { ActivatedRoute, Router } from '@angular/router';
import { AppService, Configuration } from '../../app.service';
import { NgClass, NgForOf } from '@angular/common';
import { first } from 'rxjs';

@Component({
  selector: 'app-configuration-tab-list',
  standalone: true,
  imports: [NgForOf, NgClass],
  templateUrl: './tab-list.component.html',
  styleUrl: './tab-list.component.scss',
})
export class ConfigurationTabListComponent extends TabListComponent implements OnInit {
  @Input() queryParamName: string = 'name';

  private route: ActivatedRoute = inject(ActivatedRoute);
  private router: Router = inject(Router);
  private appService: AppService = inject(AppService);

  @Input({ required: true })
  set configurations(configurations: Configuration[]) {
    this.tabs = configurations.map((configuration) => configuration.name);
  }

  ngOnInit(): void {
    this.route.queryParamMap.subscribe((parameters) => {
      const tab = parameters.get(this.queryParamName);
      if (tab) {
        this.setSelectedTab(tab);
      } else if (this.showAllTab) {
        this.setSelectedTab(this._allTabName);
      } else if (this.tabsList.length > 0) {
        this.setSelectedTab(this.tabsList[0]);
      }
    });
    this.appService.selectedConfigurationTab$.pipe(first()).subscribe((tab) => {
      if (tab !== null) this.changeTab(tab ?? this._allTabName);
    });
  }

  protected override changeTab(tab: string): void {
    this.appService.updateSelectedConfigurationTab(tab);
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: tab === this._allTabName ? { [this.queryParamName]: null } : { [this.queryParamName]: tab },
      queryParamsHandling: 'merge',
    });
  }

  private setSelectedTab(tab: string): void {
    this.selectedTab = tab;
    this.selectedTabChange.emit(tab);
  }
}
