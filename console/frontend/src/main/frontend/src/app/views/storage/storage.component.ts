import { Component, OnInit } from '@angular/core';
import { StorageService } from './storage.service';
import { SweetalertService } from 'src/app/services/sweetalert.service';
import { ActivatedRoute, ActivationEnd, Router } from '@angular/router';
import { filter, merge } from 'rxjs';

@Component({
  selector: 'app-storage',
  templateUrl: './storage.component.html',
  styleUrls: ['./storage.component.scss'],
})
export class StorageComponent implements OnInit {

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private SweetAlert: SweetalertService,
    private storageService: StorageService
  ) { }

  ngOnInit() {
    this.route.firstChild?.paramMap.subscribe(params => {
      const adapterName = params.get('adapter'),
        configuration = params.get('configuration'),
        storageSource = params.get('storageSource'),
        storageSourceName = params.get('storageSourceName'),
        processState = params.get('processState'),
        messageId = params.get('messageId');

      if (!configuration) {
        this.router.navigate(['status']);
        return;
      }

      if (!adapterName) {
        this.SweetAlert.Warning("Invalid URL", "No adapter name provided!");
        return;
      }
      if (!storageSourceName) {
        this.SweetAlert.Warning("Invalid URL", "No receiver or pipe name provided!");
        return;
      }
      if (!storageSource) {
        this.SweetAlert.Warning("Invalid URL", "Component type [receivers] or [pipes] is not provided in url!");
        return;
      }
      if (!processState) {
        this.SweetAlert.Warning("Invalid URL", "No storage type provided!");
        return;
      }

      this.storageService.updateStorageParams({
        adapterName,
        configuration,
        processState,
        storageSource,
        storageSourceName,
        messageId
      });
    });


    this.router.events.pipe(
      filter((e) => e instanceof ActivationEnd && e.snapshot.paramMap.has('processState'))
    ).subscribe(() => {
      const messageId = this.route.firstChild?.snapshot.paramMap.get('messageId');
      this.storageService.updateStorageParams({ messageId });
    });
  }

}
