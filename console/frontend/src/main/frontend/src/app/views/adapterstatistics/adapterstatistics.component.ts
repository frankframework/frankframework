import { Component, Inject, OnDestroy, OnInit, ViewChild } from '@angular/core';
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
  styleUrls: ['./adapterstatistics.component.scss']
})
export class AdapterstatisticsComponent implements OnInit, OnDestroy {
  defaults = { "name": "Name", "count": "Count", "min": "Min", "max": "Max", "avg": "Average", "stdDev": "StdDev", "sum": "Sum", "first": "First", "last": "Last" };
  adapterName: string | null = null;
  configuration: string | null = null;
  refreshing = false;
  dataset: Partial<ChartDataset<"line", number[]>> = {
    fill: false,
    backgroundColor: "#2f4050",
    pointBackgroundColor: "#2f4050",
    borderColor: "#2f4050",
    pointBorderColor: "#2f4050",
    // hoverBackgroundColor: "#2f4050",
    hoverBorderColor: "#2f4050",
  };
  hourlyStatistics: ChartData<'line', Statistics["hourly"][0]["count"][], Statistics["hourly"][0]["time"]> = {
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
          text: 'Messages Per Hour'
        },
        beginAtZero: true,
        suggestedMax: 10
      }
    },
    hover: {
      mode: 'nearest',
      intersect: true
    },
    plugins: {
      legend: {
        display: false
      },
      tooltip: {
        mode: 'index',
        intersect: false,
        displayColors: false,
      }
    }
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
    private Misc: MiscService
  ) {
    this.appConstants = this.appService.APP_CONSTANTS;
    const appConstantsSubscription = this.appService.appConstants$.subscribe(() => {
      this.appConstants = this.appService.APP_CONSTANTS;
    });
    this._subscriptions.add(appConstantsSubscription);
}

  ngOnInit() {
    const routeParams = this.route.snapshot.paramMap;
    this.adapterName = routeParams.get('name');
    this.configuration = routeParams.get('configuration');

    if (!this.adapterName) {
      this.SweetAlert.Warning("Adapter not found!");
      return;
    }

    if (this.appConstants["Statistics.boundaries"]) {
      this.populateBoundaries(); //AppConstants already loaded
    }
    else {
      const appConstantsSubscription = this.appService.appConstants$.subscribe(() => this.populateBoundaries()); //Wait for appConstants trigger to load
      this._subscriptions.add(appConstantsSubscription);
    }

    window.setTimeout(() => {
      this.refresh();
    }, 1000);
  };

  ngOnDestroy() {
    this._subscriptions.unsubscribe();
  }

  refresh() {
    this.refreshing = true;
    this.statisticsService.getStatistics(this.configuration!, this.adapterName!).subscribe((data) => {
      this.stats = data;

      let labels: string[] = [];
      let chartData: number[] = [];
      for (const index in data["hourly"]) {
        let hour = data["hourly"][index];
        labels.push(hour["time"]);
        if (this.appConstants["timezoneOffset"] != 0){
          const offsetInHours = this.appConstants["timezoneOffset"] / 60;
          const offsetIndex = +index + offsetInHours;
          const wrapCutoff = 24;
          const wrappedOffsetIndex = (wrapCutoff + offsetIndex % wrapCutoff) % wrapCutoff
          hour = data["hourly"][wrappedOffsetIndex];
        }
        chartData.push(hour["count"]);
      }
      this.hourlyStatistics.labels = labels;
      this.hourlyStatistics.datasets = [{
        data: chartData,
        ...this.dataset
      }];

      this.chart?.update();

      window.setTimeout(() => {
        this.refreshing = false;
      }, 500);
    });
  };

  populateBoundaries() {
    let timeBoundaries: string[] = this.appConstants["Statistics.boundaries"].split(",");
    let sizeBoundaries: string[] = this.appConstants["Statistics.size.boundaries"].split(",");
    let percBoundaries: string[] = this.appConstants["Statistics.percentiles"].split(",");

    let publishPercentiles = this.appConstants["Statistics.percentiles.publish"] == "true";
    let publishHistograms = this.appConstants["Statistics.histograms.publish"] == "true";
    let calculatePercentiles = this.appConstants["Statistics.percentiles.internal"] == "true";
    let displayPercentiles = publishPercentiles || publishHistograms || calculatePercentiles;

    this.Debug.info("appending Statistic.boundaries", timeBoundaries, sizeBoundaries, percBoundaries);

    const statisticsTimeBoundariesAdditive: Record<string, string> = {};
    const statisticsSizeBoundariesAdditive: Record<string, string> = {};

    for (const i in timeBoundaries) {
      let j = timeBoundaries[i];
      statisticsTimeBoundariesAdditive[j + "ms"] = "< " + j + "ms";
    }
    for (const i in sizeBoundaries) {
      let j = sizeBoundaries[i];
      statisticsSizeBoundariesAdditive[j + "B"] = "< " + j + "B";
    }
    if (displayPercentiles) {
      for (const i in percBoundaries) {
        let j = "p" + percBoundaries[i];
        statisticsTimeBoundariesAdditive[j] = j;
        statisticsSizeBoundariesAdditive[j] = j;
      }
    }
    this.statisticsTimeBoundaries = { ...this.defaults, ...statisticsTimeBoundariesAdditive };
    this.statisticsSizeBoundaries = { ...this.defaults, ...statisticsSizeBoundariesAdditive };
  }
}
