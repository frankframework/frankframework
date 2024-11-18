import { Component, inject, Input, OnInit } from '@angular/core';
import { TabListComponent } from './tab-list.component';
import { ActivatedRoute, Router } from '@angular/router';
import { Configuration } from '../../app.service';
import { NgClass, NgForOf } from '@angular/common';

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

  @Input({ required: true })
  set configurations(configurations: Configuration[]) {
    this.tabs = configurations.map((configuration) => configuration.name);
  }

  ngOnInit(): void {
    this.route.queryParamMap.subscribe((parameters) => {
      const tab = parameters.get(this.queryParamName);
      if (tab) {
        this.selectedTab = tab;
        this.selectedTabChange.emit(tab);
      } else if (this.showAllTab) {
        this.selectedTab = this._allTabName;
        this.selectedTabChange.emit(this._allTabName);
      }
    });
  }

  protected override changeTab(tab: string): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: tab === this._allTabName ? { [this.queryParamName]: null } : { [this.queryParamName]: tab },
      queryParamsHandling: 'merge',
    });
  }
}
