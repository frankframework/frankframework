import { Component, inject, OnInit } from '@angular/core';
import { StorageService } from './storage.service';
import { SweetalertService } from 'src/app/services/sweetalert.service';
import { ActivatedRoute, ActivationEnd, Router, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';

@Component({
  selector: 'app-storage',
  templateUrl: './storage.component.html',
  styleUrls: ['./storage.component.scss'],
  imports: [RouterOutlet],
})
export class StorageComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private SweetAlert = inject(SweetalertService);
  private storageService = inject(StorageService);

  ngOnInit(): void {
    this.route.firstChild?.paramMap.subscribe((parameters) => {
      const adapterName = parameters.get('adapter'),
        configuration = parameters.get('configuration'),
        storageSource = parameters.get('storageSource'),
        storageSourceName = parameters.get('storageSourceName'),
        processState = parameters.get('processState'),
        messageId = parameters.get('messageId');

      if (!configuration) {
        this.router.navigate(['status']);
        return;
      }

      if (!adapterName) {
        this.SweetAlert.warning('Invalid URL', 'No adapter name provided!');
        return;
      }
      if (!storageSourceName) {
        this.SweetAlert.warning('Invalid URL', 'No receiver or pipe name provided!');
        return;
      }
      if (!storageSource) {
        this.SweetAlert.warning('Invalid URL', 'Component type [receivers] or [pipes] is not provided in url!');
        return;
      }
      if (!processState) {
        this.SweetAlert.warning('Invalid URL', 'No storage type provided!');
        return;
      }

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
