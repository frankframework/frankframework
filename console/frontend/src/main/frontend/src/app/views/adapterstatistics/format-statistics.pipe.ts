import { Pipe, PipeTransform } from '@angular/core';

export type FormattedStatistics = {
  key: string;
  value: string | number;
  originalValue?: number;
};

@Pipe({
  name: 'formatStatistics',
})
export class FormatStatisticsPipe implements PipeTransform {
  transform(input: Record<string, string | number>, format: Record<string, string>): FormattedStatistics[] {
    const formatted: FormattedStatistics[] = [];
    for (const key in format) {
      const value: string | number = input[key];
      if (value === undefined) {
        formatted.push({ key, value: '-' });
        continue;
      }
      if (typeof value === 'string') {
        formatted.push({ key, value: value });
        continue;
      }

      const sum = input['sum'] as number;
      const count = input['count'] as number;
      const formatValue = format[key];
      let formattedValue: FormattedStatistics = { key, value };

      if (key.startsWith('p'))
        formattedValue = { key, value: this.convertToPercentage(value, sum), originalValue: value };
      else if (formatValue.startsWith('# < '))
        formattedValue = { key, value: this.convertToPercentage(value, count), originalValue: value };

      formatted.push(formattedValue);
    }
    return formatted;
  }

  private convertToPercentage(value: number, total: number): string {
    if (total === 0 || value === 0) return '0%';
    const percentage = (value / total) * 100;
    return `${percentage.toFixed(1)}%`;
  }
}
