import { KeyValue } from '@angular/common';
import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'formatStatistics',
})
export class FormatStatisticsPipe implements PipeTransform {
  transform(input: Record<string, string | number | null>, format: Record<string, string>): KeyValue<string, string>[] {
    const formatted: KeyValue<string, string>[] = [];

    for (const key in format) {
      let value = input[key];

      if (value || value === 0) {
        const sum = input['sum'] ?? null;
        const count = input['count'] ?? null;

        if (typeof value === 'number') {
          const formatValue = format[key];
          if (key.startsWith('p') && typeof sum === 'number') value = this.convertToPercentage(value, sum);
          else if (formatValue.startsWith('# < ') && typeof count === 'number')
            value = this.convertToPercentage(value, count);
        }
        formatted.push({ key, value: value as string });
        continue;
      }

      formatted.push({ key, value: '-' }); // if no value, return a dash
    }
    return formatted;
  }

  private convertToPercentage(value: number, total: number): string {
    if (total === 0) return '0%';
    const percentage = (value / total) * 100;
    return `${percentage.toFixed(1)}%`;
  }
}
