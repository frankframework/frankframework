import { ComponentFixture, TestBed } from '@angular/core/testing';

import { StorageListDtComponent } from './storage-list-dt.component';

describe('StorageListDtComponent', () => {
  let component: StorageListDtComponent;
  let fixture: ComponentFixture<StorageListDtComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ StorageListDtComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(StorageListDtComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
