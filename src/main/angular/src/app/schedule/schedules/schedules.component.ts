import {ChangeDetectorRef, Component, OnChanges, OnInit, QueryList, SimpleChanges, ViewChildren} from '@angular/core';
import {ActivatedRoute, CanDeactivate} from '@angular/router';
import {UntypedFormArray, UntypedFormGroup} from '@angular/forms';
import {DayTimeframe} from '../timeframe/day/day-timeframe';
import {TranslateService} from '@ngx-translate/core';
import {MessageBoxLevel} from '../../material/messagebox/messagebox.component';
import {ScheduleService} from '../schedule-service';
import {EvCharger} from '../../control/evcharger/ev-charger';
import {ScheduleComponent} from '../schedule.component';
import {Control} from '../../control/control';
import {EnergyRequest} from '../request/energy/energy-request';
import {RuntimeRequest} from '../request/runtime/runtime-request';
import {Schedule} from '../schedule';
import {FormHandler} from '../../shared/form-handler';
import {ConsecutiveDaysTimeframe} from '../timeframe/consecutivedays/consecutive-days-timeframe';
import {SocRequest} from '../request/soc/soc-request';
import {ElectricVehicle} from '../../control/evcharger/electric-vehicle/electric-vehicle';
import {Logger} from '../../log/logger';
import {simpleTimeframeType} from '../../shared/form-util';
import {ControlComponent} from '../../control/control.component';
import {Observable} from 'rxjs';
import {DialogService} from '../../shared/dialog.service';
import {MeterReportingSwitch} from '../../control/meterreporting/meter-reporting-switch';
import {AlwaysOnSwitch} from '../../control/alwayson/always-on-switch';

@Component({
  selector: 'app-schedules',
  templateUrl: './schedules.component.html',
  styleUrls: ['./schedules.component.scss'],
})
export class SchedulesComponent implements OnChanges, OnInit, CanDeactivate<ControlComponent> {
  @ViewChildren('scheduleComponents')
  scheduleComps: QueryList<ScheduleComponent>;
  form: UntypedFormGroup;
  formHandler: FormHandler;
  schedules: Schedule[];
  control: Control;
  applianceId: string;
  timeframeTypes: { key: string, value?: string }[] = [
    {key: DayTimeframe.TYPE},
    {key: ConsecutiveDaysTimeframe.TYPE},
  ];
  evRequestTypes: { key: string, value?: string }[] = [
    {key: EnergyRequest.TYPE},
    {key: SocRequest.TYPE},
  ];
  nonEvRequestTypes: { key: string, value?: string }[] = [
    {key: RuntimeRequest.TYPE}
  ];
  discardChangesMessage: string;
  MessageBoxLevel = MessageBoxLevel;

  constructor(private logger: Logger,
              private route: ActivatedRoute,
              private scheduleService: ScheduleService,
              private dialogService: DialogService,
              private translate: TranslateService,
              private changeDetectorRef: ChangeDetectorRef
  ) {
    this.formHandler = new FormHandler();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.control && changes.schedules.currentValue) {
      this.schedules = changes.schedules.currentValue;
    }
  }

  ngOnInit() {
    this.translate.get('dialog.candeactivate').subscribe(translated => this.discardChangesMessage = translated);
    this.route.paramMap.subscribe(() => this.applianceId = this.route.snapshot.paramMap.get('id'));
    this.route.data.subscribe((data: { schedules: Schedule[], control: Control }) => {
      this.schedules = data.schedules.map(schedule => new Schedule({
        enabled: schedule.enabled,
        timeframe: schedule.timeframe,
        request: schedule.request,
      }));
      this.control = data.control;
      this.form = this.buildForm();
    });
    const timeframeTypeKeys = this.timeframeTypes.map(timeframeType => timeframeType.key);
    this.translate.get(timeframeTypeKeys).subscribe(
      translatedKeys => {
        this.timeframeTypes.forEach(timeframeType => {
          timeframeType.value = translatedKeys[timeframeType.key];
          timeframeType.key = simpleTimeframeType(timeframeType.key);
        });
      });

    const requestTypeKeys = [...this.evRequestTypes, ...this.nonEvRequestTypes].map((requestType) => requestType.key);
    this.translate.get(requestTypeKeys).subscribe(
      translatedKeys => {
        this.evRequestTypes.forEach(requestType => {
          requestType.value = translatedKeys[requestType.key];
          requestType.key = simpleTimeframeType(requestType.key);
        });
        this.nonEvRequestTypes.forEach(requestType => {
          requestType.value = translatedKeys[requestType.key];
          requestType.key = simpleTimeframeType(requestType.key);
        });
      });
  }

  get validRequestTypes() {
    if (this.isEvCharger) {
      if (this.hasElectricVehicles) {
        return this.evRequestTypes;
      }
      return [];
    }
    return this.nonEvRequestTypes;
  }

  get isControlable(): boolean {
    return this.control && this.control.type !== MeterReportingSwitch.TYPE && this.control.type !== AlwaysOnSwitch.TYPE;
  }

  get isEvCharger(): boolean {
    return this.control ? this.control.type === EvCharger.TYPE : false;
  }

  get hasElectricVehicles(): boolean {
    return this.isEvCharger && this.electricVehicles.length > 0;
  }

  get electricVehicles(): ElectricVehicle[] {
    return this.control.evCharger ? this.control.evCharger.vehicles : [];
  }

  addSchedule() {
    this.schedules.push(new Schedule({enabled: true}));
    this.schedulesFormArray.push(this.createSchedulesFormGroup());
    this.form.markAsDirty();
    this.changeDetectorRef.detectChanges();
  }

  onScheduleRemove(index: number) {
    this.schedules.splice(index, 1);
    this.schedulesFormArray.removeAt(index);
    this.form.markAsDirty();
  }

  get schedulesFormArray() {
    return this.form.controls.schedules as UntypedFormArray;
  }

  createSchedulesFormGroup(): UntypedFormGroup {
    return new UntypedFormGroup({});
  }

  getScheduleFormGroup(index: number) {
    return this.schedulesFormArray.controls[index];
  }

  canDeactivate(): Observable<boolean> | boolean {
    if (this.form.pristine) {
      return true;
    }
    return this.dialogService.confirm(this.discardChangesMessage);
  }

  buildForm(): UntypedFormGroup {
    const schedulesFormArray = new UntypedFormArray([]);
    this.schedules.forEach(() => schedulesFormArray.push(this.createSchedulesFormGroup()));

    const form = new UntypedFormGroup({
      schedules: schedulesFormArray
    });
    return form;
  }

  submitForm() {
    if (this.scheduleComps) {
      this.schedules = [];
      this.scheduleComps.forEach(scheduleComponent => {
        const schedule = scheduleComponent.updateModelFromForm();
        if (schedule) {
          this.schedules.push(schedule);
        }
      });
    }
    this.scheduleService.setSchedules(this.applianceId, this.schedules).subscribe();
    this.form.markAsPristine();
  }
}
