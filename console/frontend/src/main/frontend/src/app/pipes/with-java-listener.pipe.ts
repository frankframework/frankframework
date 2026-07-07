import { Pipe, PipeTransform } from '@angular/core';
import { Adapter } from '../app.service';

@Pipe({
  name: 'withJavaListener',
})
export class WithJavaListenerPipe implements PipeTransform {
  transform(adapters: Record<string, Adapter>): Adapter[] {
    if (!adapters) return [];

    return Object.values(adapters).filter((adapter) => {
      const receivers = adapter.receivers ?? [];
      return receivers.some((receiver) => receiver.listener.class.startsWith('JavaListener'));
    });
  }
}
