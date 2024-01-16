import { ComponentFixture, TestBed } from '@angular/core/testing';

import { IframeCustomViewComponent } from './iframe-custom-view.component';

describe('IframeCustomViewComponent', () => {
  let component: IframeCustomViewComponent;
  let fixture: ComponentFixture<IframeCustomViewComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ IframeCustomViewComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(IframeCustomViewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
