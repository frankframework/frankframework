import { ComponentFixture, TestBed } from '@angular/core/testing';

import { IframeLadybugComponent } from './iframe-ladybug.component';

describe('IframeLadybugComponent', () => {
  let component: IframeLadybugComponent;
  let fixture: ComponentFixture<IframeLadybugComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ IframeLadybugComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(IframeLadybugComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
