import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MonitorsService } from '../monitors.service';
import { AppService, Configuration } from '../../../app.service';
import { FormsModule } from '@angular/forms';
import { NgForOf } from '@angular/common';
import { combineLatestWith, Subscription } from 'rxjs';
import { LaddaModule } from 'angular2-ladda';

@Component({
  selector: 'app-monitors-new',
  standalone: true,
  imports: [RouterLink, FormsModule, NgForOf, LaddaModule],
  templateUrl: './monitors-new.component.html',
  styleUrl: './monitors-new.component.scss',
})
export class MonitorsNewComponent implements OnInit, OnDestroy {
  protected selectedConfiguration: string = '';
  protected monitorName: string = '';
  protected ladda: boolean = false;

  private appService: AppService = inject(AppService);

  protected configurations: Configuration[] = this.appService.configurations;

  private monitorsService: MonitorsService = inject(MonitorsService);
  private router: Router = inject(Router);
  private route: ActivatedRoute = inject(ActivatedRoute);
  private subscriptions: Subscription = new Subscription();

  ngOnInit(): void {
    const configurationsSubscription = this.appService.configurations$.subscribe((configurations) => {
      this.configurations = configurations;
    });
    this.subscriptions.add(configurationsSubscription);

    this.route.paramMap.pipe(combineLatestWith(this.route.queryParamMap)).subscribe(([, queryParameters]) => {
      if (queryParameters.has('configuration')) {
        this.selectedConfiguration = queryParameters.get('configuration')!;
      }
    });
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  submit(): void {
    this.ladda = true;
    this.monitorsService
      .postMonitor(this.selectedConfiguration, {
        name: this.monitorName,
      })
      .subscribe(() => {
        this.router.navigate(['monitors'], {
          queryParams: { configuration: this.selectedConfiguration },
        });
      });
  }
}
