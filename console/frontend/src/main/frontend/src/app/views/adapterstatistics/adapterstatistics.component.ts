import { Component, inject, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import type { ChartDataset } from 'chart.js';
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
  CountPerReceiverStatistics,
  HourlyStatistics,
  SLOStatistics,
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
  protected sloStatistics: SLOStatistics = {
    labels: [],
    datasets: [],
  };
  protected countPerReceiverStatistics: CountPerReceiverStatistics = {
    labels: [],
    datasets: [],
  };
  protected iboxExpanded = {
    processReceivers: true,
    durationPerPipe: true,
    sizePerPipe: true,
  };
  protected stats?: Statistics;

  protected statisticsTimeBoundaries: Record<string, string>;
  protected statisticsSizeBoundaries: Record<string, string>;

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
  protected statisticsTimeSLOLabels: Record<string, string> = { ...this.defaults };
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
  private datasetDefaults: Partial<ChartDataset<'line', number[]>> = {
    backgroundColor: 'rgba(47, 64, 80, 0.2)',
    pointBackgroundColor: '#2f4050',
    borderColor: '#2f4050',
    pointBorderColor: '#2f4050',
    // hoverBackgroundColor: "#2f4050",
    hoverBorderColor: '#2f4050',
  };
  private hourlyStatisticsDataset: Partial<ChartDataset<'line', number[]>> = {
    ...this.datasetDefaults,
    fill: {
      target: 'origin',
    },
  };
  private sloStatisticsDataset: Partial<ChartDataset<'line', number[]>> = {
    ...this.datasetDefaults,
    fill: {
      target: { value: 100 },
      below: 'rgba(237, 85, 101, 0.2)',
    },
  };
  private countPerReceiverStatisticsDataset: Partial<ChartDataset<'doughnut', number>> = {};
  private defaultDoughnutColors = [
    // from https://github.com/chartjs/Chart.js/blob/master/src/plugins/plugin.colors.ts
    'rgb(54, 162, 235)', // blue
    'rgb(255, 99, 132)', // red
    'rgb(255, 159, 64)', // orange
    'rgb(255, 205, 86)', // yellow
    'rgb(75, 192, 192)', // green
    'rgb(153, 102, 255)', // purple
    'rgb(201, 203, 207)', // grey
  ];

  constructor() {
    this.statisticsTimeBoundaries = { ...this.defaults };
    this.statisticsSizeBoundaries = { ...this.defaults };
  }

  ngOnInit(): void {
    const routeParameters = this.route.snapshot.paramMap;
    this.adapterName = routeParameters.get('name');
    this.configuration = routeParameters.get('configuration');

    if (!this.adapterName) {
      this.SweetAlert.warning('Adapter not found!');
      return;
    }

    this.appConstantsSubscription = this.appConstants$.subscribe((appConstants) =>
      this.populateBoundaries(appConstants),
    );

    globalThis.setTimeout(() => {
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
      this.setHourlyStatisticsData(data);
      this.setSLOStatisticsData(data);
      this.setCountPerReceiverStatisticsData(data);

      this.chart?.update();

      globalThis.setTimeout(() => {
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

  private setHourlyStatisticsData(data: Statistics): void {
    const labels: string[] = [];
    const chartData: number[] = [];
    const currentServerHour = this.serverTimeService.getDateWithOffset().getHours();
    const rotatedData = this.rotateHourlyData(data['hourly'], currentServerHour + 1);
    for (const hour of rotatedData) {
      labels.push(hour.time);
      chartData.push(hour.count);
    }
    this.hourlyStatistics = {
      labels,
      datasets: [
        {
          data: chartData,
          ...this.hourlyStatisticsDataset,
        },
      ],
    };
  }

  private setSLOStatisticsData(data: Statistics): void {
    const chartData: number[] = [];
    for (const key of Object.keys(this.statisticsTimeSLOLabels)) {
      chartData.push(
        ((data.totalMessageProccessingTime[key as keyof Statistics['totalMessageProccessingTime']] as number) /
          data.totalMessageProccessingTime.count) *
          100,
      );
    }
    this.sloStatistics = {
      labels: Object.values(this.statisticsTimeSLOLabels),
      datasets: [
        {
          label: 'Duration',
          data: chartData,
          ...this.sloStatisticsDataset,
        },
      ],
    };
  }

  private setCountPerReceiverStatisticsData(data: Statistics): void {
    const names = data.receivers.map((receiver) => receiver.name);
    const counts = data.receivers.map((receiver) => receiver.messagesReceived);
    const retries = data.receivers.flatMap((receiver) => [
      receiver.messagesReceived - receiver.messagesRetried,
      receiver.messagesRetried,
    ]);
    this.countPerReceiverStatistics = {
      labels: names,
      datasets: [
        {
          data: counts,
          backgroundColor: this.defaultDoughnutColors,
        },
        {
          data: retries,
          backgroundColor: ['rgba(26, 179, 148, 0.65)', 'rgba(237, 85, 101, 0.65)'],
        },
      ],
    };
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

    const statisticsTimeSLOLabels: Record<string, string> = {};
    const statisticsTimeBoundariesAdditive: Record<string, string> = {};
    const statisticsSizeBoundariesAdditive: Record<string, string> = {};

    for (const index in timeBoundaries) {
      const index_ = `${timeBoundaries[index]}ms`;
      const label = `# < ${index_}`;
      statisticsTimeBoundariesAdditive[index_] = label;
      statisticsTimeSLOLabels[index_] = label;
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

    this.statisticsTimeSLOLabels = statisticsTimeSLOLabels;
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
    return [...items.slice(amount), ...items.slice(0, amount)];
  }
}
