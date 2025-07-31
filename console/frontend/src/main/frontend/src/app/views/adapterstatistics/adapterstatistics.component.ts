import { Component, inject, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import type { ChartData, ChartDataset, ChartOptions } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';
import { Subscription } from 'rxjs';
import { AppConstants, AppService } from 'src/app/app.service';
import { DebugService } from 'src/app/services/debug.service';
import { SweetalertService } from 'src/app/services/sweetalert.service';
import { AdapterstatisticsService, Statistics, StatisticsKeeper } from './adapterstatistics.service';
import { LaddaModule } from 'angular2-ladda';

import { FormatStatKeysPipe } from './format-stat-keys.pipe';
import { FormatStatisticsPipe } from './format-statistics.pipe';
import { ServerTimeService } from '../../services/server-time.service';
import { toObservable } from '@angular/core/rxjs-interop';
import {
  AdapterstatisticsChartsComponent,
  HourlyStatistics,
} from './adapterstatistics-charts/adapterstatistics-charts.component';

@Component({
  selector: 'app-adapterstatistics',
  imports: [LaddaModule, RouterLink, FormatStatKeysPipe, FormatStatisticsPipe, AdapterstatisticsChartsComponent],
  templateUrl: './adapterstatistics.component.html',
  styleUrls: ['./adapterstatistics.component.scss'],
})
export class AdapterstatisticsComponent implements OnInit, OnDestroy {
  @ViewChild(BaseChartDirective) chart?: BaseChartDirective;

  protected adapterName: string | null = null;
  protected configuration: string | null = null;
  protected refreshing = false;
  protected hourlyStatistics: HourlyStatistics = {
    labels: [],
    datasets: [],
  };
  protected iboxExpanded = {
    processReceivers: true,
    durationPerPipe: true,
    sizePerPipe: true,
  };
  protected stats?: Statistics;

  private defaults = {
    name: 'Name',
    count: 'Count',
    min: 'Min',
    max: 'Max',
    avg: 'Average',
    stdDev: 'StdDev',
    sum: 'Sum',
    first: 'First',
    last: 'Last',
  };
  protected statisticsTimeBoundaries: Record<string, string> = { ...this.defaults };
  protected statisticsSizeBoundaries: Record<string, string> = { ...this.defaults };

  private readonly appService: AppService = inject(AppService);
  private readonly route: ActivatedRoute = inject(ActivatedRoute);
  private readonly statisticsService: AdapterstatisticsService = inject(AdapterstatisticsService);
  private readonly SweetAlert: SweetalertService = inject(SweetalertService);
  private readonly Debug: DebugService = inject(DebugService);
  private readonly serverTimeService: ServerTimeService = inject(ServerTimeService);
  private appConstants$ = toObservable(this.appService.appConstants);
  private appConstantsSubscription: Subscription | null = null;
  private dataset: Partial<ChartDataset<'line', number[]>> = {
    fill: false,
    backgroundColor: '#2f4050',
    pointBackgroundColor: '#2f4050',
    borderColor: '#2f4050',
    pointBorderColor: '#2f4050',
    // hoverBackgroundColor: "#2f4050",
    hoverBorderColor: '#2f4050',
  };

  ngOnInit(): void {
    const routeParameters = this.route.snapshot.paramMap;
    this.adapterName = routeParameters.get('name');
    this.configuration = routeParameters.get('configuration');

    if (!this.adapterName) {
      this.SweetAlert.Warning('Adapter not found!');
      return;
    }

    this.appConstantsSubscription = this.appConstants$.subscribe((appConstants) =>
      this.populateBoundaries(appConstants),
    );

    window.setTimeout(() => {
      this.refresh();
    });
  }

  ngOnDestroy(): void {
    this.appConstantsSubscription?.unsubscribe();
  }

  refresh(): void {
    this.refreshing = true;
    this.statisticsService.getStatistics(this.configuration!, this.adapterName!).subscribe((data) => {
      this.stats = data;
      const labels: string[] = [];
      const chartData: number[] = [];
      const currentServerHour = this.serverTimeService.getDateWithOffset().getHours();
      const rotatedData = this.rotateHourlyData(data['hourly'], currentServerHour + 1);
      for (const hour of rotatedData) {
        labels.push(hour.time);
        chartData.push(hour.count);
      }
      this.hourlyStatistics.labels = labels;
      this.hourlyStatistics.datasets = [
        {
          data: chartData,
          ...this.dataset,
        },
      ];

      this.chart?.update();

      window.setTimeout(() => {
        this.refreshing = false;
      });
    });
  }

  collapseExpand(key: keyof typeof this.iboxExpanded): void {
    this.iboxExpanded[key] = !this.iboxExpanded[key];
  }

  getSortedProcessingThreads(receiver: Statistics['receivers'][0]): StatisticsKeeper[] {
    return receiver.processing.sort((a: StatisticsKeeper, b: StatisticsKeeper) => a.name.localeCompare(b.name));
  }

  private populateBoundaries(appConstants: AppConstants): void {
    if (!appConstants['Statistics.boundaries']) return;

    const timeBoundaries: string[] = (appConstants['Statistics.boundaries'] as string).split(',');
    const sizeBoundaries: string[] = (appConstants['Statistics.size.boundaries'] as string).split(',');
    const percBoundaries: string[] = (appConstants['Statistics.percentiles'] as string).split(',');

    const publishPercentiles = appConstants['Statistics.percentiles.publish'] == 'true';
    const publishHistograms = appConstants['Statistics.histograms.publish'] == 'true';
    const displayPercentiles = publishPercentiles || publishHistograms;

    this.Debug.info('appending Statistic.boundaries', timeBoundaries, sizeBoundaries, percBoundaries);

    const statisticsTimeBoundariesAdditive: Record<string, string> = {};
    const statisticsSizeBoundariesAdditive: Record<string, string> = {};

    for (const index in timeBoundaries) {
      const index_ = timeBoundaries[index];
      statisticsTimeBoundariesAdditive[`${index_}ms`] = `# < ${index_}ms`;
    }
    for (const index in sizeBoundaries) {
      const index_ = sizeBoundaries[index];
      statisticsSizeBoundariesAdditive[`${index_}B`] = `# < ${index_}B`;
    }
    if (displayPercentiles) {
      for (const index in percBoundaries) {
        const index_ = `p${percBoundaries[index]}`;
        statisticsTimeBoundariesAdditive[index_] = index_;
        statisticsSizeBoundariesAdditive[index_] = index_;
      }
    }
    this.statisticsTimeBoundaries = {
      ...this.defaults,
      ...statisticsTimeBoundariesAdditive,
    };
    this.statisticsSizeBoundaries = {
      ...this.defaults,
      ...statisticsSizeBoundariesAdditive,
    };
  }

  private rotateHourlyData<T>(items: T[], amount: number): T[] {
    amount = amount % items.length;
    return [...items.slice(amount, items.length), ...items.slice(0, amount)];
  }
}
