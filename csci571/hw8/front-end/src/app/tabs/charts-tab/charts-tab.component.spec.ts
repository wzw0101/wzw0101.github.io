import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ChartsTabComponent } from './charts-tab.component';

describe('ChartsTabComponent', () => {
  let component: ChartsTabComponent;
  let fixture: ComponentFixture<ChartsTabComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ChartsTabComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ChartsTabComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
