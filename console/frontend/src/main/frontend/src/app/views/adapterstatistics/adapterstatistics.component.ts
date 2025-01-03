import { Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import type { ChartData, ChartDataset, ChartOptions } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';
import { Subscription } from 'rxjs';
import { AppConstants, AppService } from 'src/app/app.service';
import { DebugService } from 'src/app/services/debug.service';
import { MiscService } from 'src/app/services/misc.service';
import { SweetalertService } from 'src/app/services/sweetalert.service';
import { AdapterstatisticsService, Statistics } from './adapterstatistics.service';

@Component({
  selector: 'app-adapterstatistics',
  templateUrl: './adapterstatistics.component.html',
  styleUrls: ['./adapterstatistics.component.scss'],
  standalone: false,
})
export class AdapterstatisticsComponent implements OnInit, OnDestroy {
  defaults = {
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
  adapterName: string | null = null;
  configuration: string | null = null;
  refreshing = false;
  dataset: Partial<ChartDataset<'line', number[]>> = {
    fill: false,
    backgroundColor: '#2f4050',
    pointBackgroundColor: '#2f4050',
    borderColor: '#2f4050',
    pointBorderColor: '#2f4050',
    // hoverBackgroundColor: "#2f4050",
    hoverBorderColor: '#2f4050',
  };
  hourlyStatistics: ChartData<'line', Statistics['hourly'][0]['count'][], Statistics['hourly'][0]['time']> = {
    labels: [],
    datasets: [],
  };
  stats?: Statistics;
  statisticsTimeBoundaries: Record<string, string> = { ...this.defaults };
  statisticsSizeBoundaries: Record<string, string> = { ...this.defaults };
  statisticsNames = [];
  options: ChartOptions<'line'> = {
    elements: {
      line: {
        tension: 0.5,
      },
    },
    responsive: true,
    maintainAspectRatio: false,
    scales: {
      yAxis: {
        display: true,
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

  private _subscriptions = new Subscription();
  private appConstants: AppConstants;

  @ViewChild(BaseChartDirective) chart?: BaseChartDirective;

  constructor(
    private appService: AppService,
    private route: ActivatedRoute,
    private statisticsService: AdapterstatisticsService,
    private SweetAlert: SweetalertService,
    private Debug: DebugService,
    private Misc: MiscService,
  ) {
    this.appConstants = this.appService.APP_CONSTANTS;
    const appConstantsSubscription = this.appService.appConstants$.subscribe(() => {
      this.appConstants = this.appService.APP_CONSTANTS;
    });
    this._subscriptions.add(appConstantsSubscription);
  }

  ngOnInit(): void {
    const routeParameters = this.route.snapshot.paramMap;
    this.adapterName = routeParameters.get('name');
    this.configuration = routeParameters.get('configuration');

    if (!this.adapterName) {
      this.SweetAlert.Warning('Adapter not found!');
      return;
    }

    if (this.appConstants['Statistics.boundaries']) {
      this.populateBoundaries(); //AppConstants already loaded
    } else {
      const appConstantsSubscription = this.appService.appConstants$.subscribe(() => this.populateBoundaries()); //Wait for appConstants trigger to load
      this._subscriptions.add(appConstantsSubscription);
    }

    window.setTimeout(() => {
      this.refresh();
    }, 1000);
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
      for (const hour of data['hourly']) {
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
      }, 500);
    });
  }

  populateBoundaries(): void {
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
}
