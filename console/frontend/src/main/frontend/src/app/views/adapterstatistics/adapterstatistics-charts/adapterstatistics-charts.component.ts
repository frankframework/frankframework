import { Component, input } from '@angular/core';
import { BaseChartDirective } from 'ng2-charts';
import type { ChartData, ChartOptions } from 'chart.js';

export type HourlyStatistics = ChartData<'line', number[], string>;
export type SLOStatistics = ChartData<'line', number[], string>;
export type CountPerReceiverStatistics = ChartData<'doughnut', number[], string>;

@Component({
  selector: 'app-adapterstatistics-charts',
  imports: [BaseChartDirective],
  templateUrl: './adapterstatistics-charts.component.html',
  styleUrl: './adapterstatistics-charts.component.scss',
})
export class AdapterstatisticsChartsComponent {
  public readonly hourlyStatistics = input<HourlyStatistics>({
    labels: [],
    datasets: [],
  });
  public readonly sloStatistics = input<SLOStatistics>({
    labels: [],
    datasets: [],
  });
  public readonly countPerReceiverStatistics = input<CountPerReceiverStatistics>({
    labels: [],
    datasets: [],
  });

  protected defaultLineOptions: ChartOptions<'line'> = {
    responsive: true,
    maintainAspectRatio: false,
    elements: {
      line: {
        tension: 0.5,
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

  protected hourlyStatisticsOptions: ChartOptions<'line'> = {
    ...this.defaultLineOptions,
    scales: {
      y: {
        title: {
          display: true,
          text: 'Messages Per Hour',
        },
        beginAtZero: true,
        suggestedMin: 0,
        suggestedMax: 10,
      },
    },
  };

  protected sloStatisticsOptions: ChartOptions<'line'> = {
    ...this.defaultLineOptions,
    scales: {
      y: {
        title: {
          display: true,
          text: '% of Messages',
        },
        suggestedMin: 0,
        suggestedMax: 100,
      },
    },
  };

  protected countPerReceiverStatisticsOptions: ChartOptions<'doughnut'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'right',
      },
      tooltip: {
        intersect: false,
        displayColors: false,
        filter: (tooltipItem) => tooltipItem.datasetIndex === 0,
      },
    },
  };
}
