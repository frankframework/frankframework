import { Component, input, InputSignal, OnInit } from '@angular/core';
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
export class AdapterstatisticsChartsComponent implements OnInit {
  public readonly hourlyStatistics: InputSignal<HourlyStatistics> = input<HourlyStatistics>({
    labels: [],
    datasets: [],
  });
  public readonly test: InputSignal<ChartData<'doughnut'>> = input<ChartData<'doughnut'>>({
    labels: [],
    datasets: [],
  });

  protected hourlyStatisticsOptions: ChartOptions<'line'> = {
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

  ngOnInit(): void {
    console.log(this.hourlyStatistics());
  }
}
