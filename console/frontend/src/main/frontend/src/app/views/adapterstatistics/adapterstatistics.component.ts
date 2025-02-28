import { Component, inject, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import type { ChartData, ChartDataset, ChartOptions } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';
import { Subscription } from 'rxjs';
import { AppConstants, AppService } from 'src/app/app.service';
import { DebugService } from 'src/app/services/debug.service';
import { SweetalertService } from 'src/app/services/sweetalert.service';
import { AdapterstatisticsService, Statistics } from './adapterstatistics.service';
import { LaddaModule } from 'angular2-ladda';

import { FormatStatKeysPipe } from './format-stat-keys.pipe';
import { FormatStatisticsPipe } from './format-statistics.pipe';

@Component({
  selector: 'app-adapterstatistics',
  imports: [LaddaModule, RouterLink, BaseChartDirective, FormatStatKeysPipe, FormatStatisticsPipe],
  templateUrl: './adapterstatistics.component.html',
  styleUrls: ['./adapterstatistics.component.scss'],
})
export class AdapterstatisticsComponent implements OnInit, OnDestroy {
  @ViewChild(BaseChartDirective) chart?: BaseChartDirective;

  protected adapterName: string | null = null;
  protected configuration: string | null = null;
  protected refreshing = false;
  protected hourlyStatistics: ChartData<'line', Statistics['hourly'][0]['count'][], Statistics['hourly'][0]['time']> = {
    labels: [],
    datasets: [],
  };
  protected stats?: Statistics;
  protected options: ChartOptions<'line'> = {
    elements: {
      line: {
        tension: 0.5,
      },
    },
    responsive: true,
    maintainAspectRatio: false,
    scales: {
      y: {
        title: {
          display: true,
          text: 'Messages Per Hour',
        },
        beginAtZero: true,
        suggestedMax: 10,
      },
    },
    hover: {
      mode: 'nearest',
      intersect: true,
    },
    plugins: {
      legend: {
        display: false,
      },
      tooltip: {
        mode: 'index',
        intersect: false,
        displayColors: false,
      },
    },
  };
  protected iboxExpanded = {
    processReceivers: true,
    durationPerPipe: true,
    sizePerPipe: true,
  };

  private _subscriptions = new Subscription();
  private dataset: Partial<ChartDataset<'line', number[]>> = {
    fill: false,
    backgroundColor: '#2f4050',
    pointBackgroundColor: '#2f4050',
    borderColor: '#2f4050',
    pointBorderColor: '#2f4050',
    // hoverBackgroundColor: "#2f4050",
    hoverBorderColor: '#2f4050',
  };

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

  private appService: AppService = inject(AppService);
  private route: ActivatedRoute = inject(ActivatedRoute);
  private statisticsService: AdapterstatisticsService = inject(AdapterstatisticsService);
  private SweetAlert: SweetalertService = inject(SweetalertService);
  private Debug: DebugService = inject(DebugService);
  private appConstants: AppConstants = this.appService.APP_CONSTANTS;

  ngOnInit(): void {
    const routeParameters = this.route.snapshot.paramMap;
    this.adapterName = routeParameters.get('name');
    this.configuration = routeParameters.get('configuration');

    if (!this.adapterName) {
      this.SweetAlert.Warning('Adapter not found!');
      return;
    }

    const appConstantsSubscription = this.appService.appConstants$.subscribe(() => {
      this.appConstants = this.appService.APP_CONSTANTS;
      this.populateBoundaries();
    });
    this._subscriptions.add(appConstantsSubscription);

    if (this.appConstants['Statistics.boundaries']) {
      this.populateBoundaries(); //AppConstants already loaded
    }

    window.setTimeout(() => {
      this.refresh();
    });
  }

  ngOnDestroy(): void {
    this._subscriptions.unsubscribe();
  }

  refresh(): void {
    this.refreshing = true;
    this.statisticsService.getStatistics(this.configuration!, this.adapterName!).subscribe((data) => {
      this.stats = data;
      const labels: string[] = [];
      const chartData: number[] = [];
      const currentHour = new Date().getHours();
      const rotatedData = this.rotateHourlyData(data['hourly'], currentHour + 1);
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

  private populateBoundaries(): void {
    const timeBoundaries: string[] = (this.appConstants['Statistics.boundaries'] as string).split(',');
    const sizeBoundaries: string[] = (this.appConstants['Statistics.size.boundaries'] as string).split(',');
    const percBoundaries: string[] = (this.appConstants['Statistics.percentiles'] as string).split(',');

    const publishPercentiles = this.appConstants['Statistics.percentiles.publish'] == 'true';
    const publishHistograms = this.appConstants['Statistics.histograms.publish'] == 'true';
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
