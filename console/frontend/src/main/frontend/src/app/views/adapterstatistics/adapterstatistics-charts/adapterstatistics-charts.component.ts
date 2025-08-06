import { Component, input, InputSignal, OnInit } from '@angular/core';
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
export class AdapterstatisticsChartsComponent implements OnInit {
  public readonly hourlyStatistics: InputSignal<HourlyStatistics> = input<HourlyStatistics>({
    labels: [],
    datasets: [],
  });
  public readonly sloStatistics: InputSignal<ChartData<'line'>> = input<ChartData<'line'>>({
    labels: [],
    datasets: [],
  });
  public readonly countPerReceiverStatistics: InputSignal<ChartData<'doughnut'>> = input<ChartData<'doughnut'>>({
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
        suggestedMax: 10,
      },
    },
  };

  ngOnInit(): void {
    console.log(this.hourlyStatistics());
  }
}
