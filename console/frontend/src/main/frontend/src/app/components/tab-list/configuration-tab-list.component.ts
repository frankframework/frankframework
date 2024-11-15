import { Component, inject, OnInit } from '@angular/core';
import { TabListComponent } from './tab-list.component';
import { ActivatedRoute, Router } from '@angular/router';

@Component({
  selector: 'app-configuration-tab-list',
  standalone: true,
  imports: [],
  templateUrl: './tab-list.component.html',
  styleUrl: './tab-list.component.scss',
})
export class ConfigurationTabListComponent extends TabListComponent implements OnInit {
  private route: ActivatedRoute = inject(ActivatedRoute);
  private router: Router = inject(Router);

  ngOnInit(): void {
    this.route.queryParamMap.subscribe((parameters) => {
      this.changeTab(parameters.get('name') ?? 'All');
    });
  }

  protected override changeTab(tab: string): void {
    super.changeTab(tab);
    this.router.navigate([], { relativeTo: this.route, queryParams: { name: tab }, queryParamsHandling: 'merge' });
  }
}
