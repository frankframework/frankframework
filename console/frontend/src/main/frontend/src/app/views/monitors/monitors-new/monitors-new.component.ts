import { Component, computed, inject, OnDestroy, OnInit, Signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MonitorsService } from '../monitors.service';
import { AppService } from '../../../app.service';
import { FormsModule } from '@angular/forms';
import { combineLatestWith, Subscription } from 'rxjs';
import { LaddaModule } from 'angular2-ladda';
import { ComboboxComponent, Option } from '../../../components/combobox/combobox.component';
import { QuickSubmitFormDirective } from '../../../components/quick-submit-form.directive';

@Component({
  selector: 'app-monitors-new',
  imports: [RouterLink, FormsModule, LaddaModule, ComboboxComponent, QuickSubmitFormDirective],
  templateUrl: './monitors-new.component.html',
  styleUrl: './monitors-new.component.scss',
})
export class MonitorsNewComponent implements OnInit, OnDestroy {
  protected selectedConfiguration = '';
  protected monitorName = '';
  protected ladda = false;
  protected configurations: Signal<Option[]> = computed(() =>
    this.appService.configurations().map((config) => ({ label: config.name })),
  );

  private readonly appService: AppService = inject(AppService);
  private readonly monitorsService: MonitorsService = inject(MonitorsService);
  private readonly router: Router = inject(Router);
  private readonly route: ActivatedRoute = inject(ActivatedRoute);
  private routeSubscription: Subscription | null = null;

  ngOnInit(): void {
    this.routeSubscription = this.route.paramMap
      .pipe(combineLatestWith(this.route.queryParamMap))
      .subscribe(([, queryParameters]) => {
        if (queryParameters.has('configuration')) {
          this.selectedConfiguration = queryParameters.get('configuration')!;
        }
      });
  }

  ngOnDestroy(): void {
    this.routeSubscription?.unsubscribe();
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
