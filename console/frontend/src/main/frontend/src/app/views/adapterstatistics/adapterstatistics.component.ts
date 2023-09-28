import { Component, Inject, OnInit, ViewChild } from '@angular/core';
import { StateParams } from '@uirouter/angularjs';
import type { ChartData, ChartDataset, ChartOptions, ChartType } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';
import { AppConstants } from 'src/angularjs/app/app.module';
import { AppService } from 'src/angularjs/app/app.service';
import { ApiService } from 'src/angularjs/app/services/api.service';
import { DebugService } from 'src/angularjs/app/services/debug.service';
import { MiscService } from 'src/angularjs/app/services/misc.service';
import { SweetAlertService } from 'src/angularjs/app/services/sweetalert.service';
import { APPCONSTANTS } from 'src/app/app.module';

type StatisticsKeeper = {
  name: string,
  count: number,
  min: number | null,
  max: number | null,
  avg: number | null,
  stdDev: number | null,
  sum: number | null,
  first: number | null,
  last: number | null,
}

type StatisticDetails = StatisticsKeeper & {
  "p50": number | null,
  "p90": number | null,
  "p95": number | null,
  "p98": number | null
}

type StatisticDetailsTime = StatisticDetails & {
  "100ms": | null,
  "1000ms": | null,
  "2000ms": | null,
  "10000ms": | null,
}

type StatisticDetailsSize = StatisticDetails & {
  "100000B": | null,
  "1000000B": | null,
}

type Statistics = {
  types: string[],
  totalMessageProccessingTime: StatisticDetailsTime,
  receivers: {
    class: string,
    idle: unknown[],
    messagesReceived: number,
    messagesRetried: number,
    name: string,
    processing: StatisticsKeeper[]
  }[],
  durationPerPipe: StatisticDetailsTime[],
  hourly: {
    count: number,
    time: string
  }[],
  sizePerPipe: StatisticDetailsSize[],
  labels: string[]
}

@Component({
  selector: 'app-adapterstatistics',
  templateUrl: './adapterstatistics.component.html',
  styleUrls: ['./adapterstatistics.component.scss']
})
export class AdapterstatisticsComponent implements OnInit {
  defaults = { "name": "Name", "count": "Count", "min": "Min", "max": "Max", "avg": "Average", "stdDev": "StdDev", "sum": "Sum", "first": "First", "last": "Last" };
  adapterName = this.$stateParams['name'];
  configuration = this.$stateParams['configuration'];
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
  hourlyStatistics: /* {
    labels: Statistics["hourly"][0]["time"][];
    data: Statistics["hourly"][0]["count"][]
    // data: ChartData[];
  } */ ChartData<'line', Statistics["hourly"][0]["count"][], Statistics["hourly"][0]["time"]> = {
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
    // tooltips: {
    //   mode: 'index',
    //   intersect: false,
    //   displayColors: false,
    // },
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

  @ViewChild(BaseChartDirective) chart?: BaseChartDirective;

  constructor(
    private appService: AppService,
    private Api: ApiService,
    private $stateParams: StateParams,
    private SweetAlert: SweetAlertService,
    @Inject(APPCONSTANTS) private appConstants: AppConstants,
    private Debug: DebugService,
    private Misc: MiscService
  ) { }

  ngOnInit() {
    if (!this.adapterName) {
      this.SweetAlert.Warning("Adapter not found!");
      return;
    }

    if (this.appConstants["Statistics.boundaries"]) {
      this.populateBoundaries(); //AppConstants already loaded
    }
    else {
      this.appService.appConstants$.subscribe(() => this.populateBoundaries()); //Wait for appConstants trigger to load
    }

    window.setTimeout(() => {
      this.refresh();
    }, 1000);
  };

  refresh() {
    this.refreshing = true;
    this.Api.Get("configurations/" + this.Misc.escapeURL(this.configuration) + "/adapters/" + this.Misc.escapeURL(this.adapterName) + "/statistics", (data: Statistics) => {
      this.stats = data;

      let labels: string[] = [];
      let chartData: number[] = [];
      for (const i in data["hourly"]) {
        let a = data["hourly"][i];
        labels.push(a["time"]);
        chartData.push(a["count"]);
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

    for (const i in timeBoundaries) {
      let j = timeBoundaries[i];
      this.statisticsTimeBoundaries[j + "ms"] = "< " + j + "ms";
    }
    for (const i in sizeBoundaries) {
      let j = sizeBoundaries[i];
      this.statisticsSizeBoundaries[j + "B"] = "< " + j + "B";
    }
    if (displayPercentiles) {
      for (const i in percBoundaries) {
        let j = "p" + percBoundaries[i];
        this.statisticsTimeBoundaries[j] = j;
        this.statisticsSizeBoundaries[j] = j;
      }
    }
  }
}
