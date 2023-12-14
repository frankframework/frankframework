import { ComponentFixture, TestBed } from '@angular/core/testing';

import { StorageListComponent } from './storage-list.component';

describe('StorageListComponent', () => {
  let component: StorageListComponent;
  let fixture: ComponentFixture<StorageListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ StorageListComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(StorageListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
