import { Component, input, InputSignal, Signal } from '@angular/core';
import { BaseChartDirective } from 'ng2-charts';
import type { ChartData, ChartOptions } from 'chart.js';
import { Statistics } from '../adapterstatistics.service';

export type HourlyStatistics = ChartData<'line', Statistics['hourly'][0]['count'][], Statistics['hourly'][0]['time']>;

@Component({
  selector: 'app-adapterstatistics-charts',
  imports: [BaseChartDirective],
  templateUrl: './adapterstatistics-charts.component.html',
  styleUrl: './adapterstatistics-charts.component.scss',
})
export class AdapterstatisticsChartsComponent {
  public readonly hourlyStatistics: InputSignal<HourlyStatistics> = input<HourlyStatistics>({
    labels: [],
    datasets: [],
  });

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
}
