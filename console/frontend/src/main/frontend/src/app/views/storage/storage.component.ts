import { Component, inject, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { ActivatedRoute, ActivationEnd, Router, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { StorageService } from './storage.service';
import { SweetalertService } from '../../services/sweetalert.service';

@Component({
  selector: 'app-storage',
  templateUrl: './storage.component.html',
  styleUrls: ['./storage.component.scss'],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [RouterOutlet],
})
export class StorageComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private SweetAlert = inject(SweetalertService);
  private storageService = inject(StorageService);

  ngOnInit(): void {
    this.route.firstChild?.paramMap.subscribe((parameters) => {
      const configuration = parameters.get('configuration');
      if (!configuration) {
        this.router.navigate(['status']);
        return;
      }
      const adapterName = parameters.get('adapter');
      if (!adapterName) {
        this.SweetAlert.warning('Invalid URL', 'No adapter name provided!');
        return;
      }

      const storageSourceName = parameters.get('storageSourceName');
      if (!storageSourceName) {
        this.SweetAlert.warning('Invalid URL', 'No receiver or pipe name provided!');
        return;
      }
      const storageSource = parameters.get('storageSource');
      if (!storageSource) {
        this.SweetAlert.warning('Invalid URL', 'Component type [receivers] or [pipes] is not provided in url!');
        return;
      }
      const processState = parameters.get('processState');
      if (!processState) {
        this.SweetAlert.warning('Invalid URL', 'No storage type provided!');
        return;
      }

      const messageId = parameters.get('messageId');
      this.storageService.updateStorageParams({
        adapterName,
        configuration,
        processState,
        storageSource,
        storageSourceName,
        messageId,
      });
    });

    this.router.events
      .pipe(filter((event) => event instanceof ActivationEnd && event.snapshot.paramMap.has('processState')))
      .subscribe(() => {
        const messageId = this.route.firstChild?.snapshot.paramMap.get('messageId');
        this.storageService.updateStorageParams({ messageId });
      });
  }
}
