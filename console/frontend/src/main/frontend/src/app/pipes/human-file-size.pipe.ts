import { Pipe, PipeTransform } from '@angular/core';

/**
 * Format bytes as human-readable text.
 *
 * @param bytes Number of bytes.
 * @param useSiUnits True to use metric (SI) units, aka powers of 1000. False to use
 *           binary (IEC), aka powers of 1024.
 * @param decimalPlaces Number of decimal places to display.
 *
 * @return Formatted string.
 */
@Pipe({
  name: 'humanFileSize',
  standalone: true,
})
export class HumanFileSizePipe implements PipeTransform {
  transform(bytes: number, useSiUnits = false, decimalPlaces = 1): string {
    const threshold = useSiUnits ? 1000 : 1024;

    if (Math.abs(bytes) < threshold) {
      return `${bytes} B`;
    }

    const units = useSiUnits
      ? ['kB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB']
      : ['KiB', 'MiB', 'GiB', 'TiB', 'PiB', 'EiB', 'ZiB', 'YiB'];
    let index = -1;
    const roundingPrecision = 10 ** decimalPlaces;

    do {
      bytes /= threshold;
      ++index;
    } while (
      Math.round(Math.abs(bytes) * roundingPrecision) / roundingPrecision >= threshold &&
      index < units.length - 1
    );

    return `${bytes.toFixed(decimalPlaces)} ${units[index]}`;
  }
}
