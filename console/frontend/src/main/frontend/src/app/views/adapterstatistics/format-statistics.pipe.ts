import { KeyValue } from '@angular/common';
import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'formatStatistics',
})
export class FormatStatisticsPipe implements PipeTransform {
  transform(
    input: Record<string, string | number | null>,
    format: Record<string, unknown>,
  ): KeyValue<string, string>[] {
    const formatted: KeyValue<string, string>[] = [];
    for (const key in format) {
      let value = input[key];
      if (!value && value !== 0) {
        // if no value, return a dash
        value = '-';
      }
      formatted.push({ key, value: value as string });
    }
    // Uncomment if really needed, but figure out how angular2+ handles this first
    // formatted["$$hashKey"] = input["$$hashKey"]; //Copy the hashKey over so Angular doesn't trigger another digest cycle
    return formatted;
  }
}
