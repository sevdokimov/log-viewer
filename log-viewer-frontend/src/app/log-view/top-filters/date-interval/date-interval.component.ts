import {Component, ElementRef, ViewChild} from '@angular/core';
import {FilterPanelStateService, FilterState} from '@app/log-view/filter-panel-state.service';
import {NGX_MAT_DATE_FORMATS, NgxMatDateFormats} from '@angular-material-components/datetime-picker';
import {Moment} from 'moment/moment';
import * as moment from 'moment';
import {FilterWithDropdown} from '@app/log-view/top-filters/filter-with-dropdown';
import {LvUtils} from '@app/utils/utils';
import {formatDateAsNanosecondString} from "@app/log-view/top-filters/date-interval/date-interval-filter-factory";
import {LanguageService} from "@app/log-view/language-service";

const DATE_FORMAT = 'YYYY-MM-DD HH:mm:ss';

const CUSTOM_DATE_FORMATS: NgxMatDateFormats = {
    parse: {
        dateInput: DATE_FORMAT,
    },
    display: {
        dateInput: DATE_FORMAT,
        monthYearLabel: 'MMM YYYY',
        dateA11yLabel: 'LL',
        monthYearA11yLabel: 'MMMM YYYY'
    }
};

@Component({
    selector: 'lv-date-interval',
    templateUrl: './date-interval.component.html',
    styleUrls: ['./date-interval.component.scss'],

    providers: [
        {provide: NGX_MAT_DATE_FORMATS, useValue: CUSTOM_DATE_FORMATS}
    ]
})
export class LvDateIntervalComponent extends FilterWithDropdown {

    @ViewChild('dateDropDown', {static: true})
    private dateDropDownElement: ElementRef;

    title: string;

    endDate: Moment;
    startDate: Moment;

    defaultDate: Moment;

    languageService: LanguageService;

    readonly defaultTime = [0, 0, 0];

    constructor(filterPanelStateService: FilterPanelStateService, languageService: LanguageService) {
        super(filterPanelStateService);
        this.languageService = languageService;
    }

    protected getDropdownDiv(): ElementRef {
        return this.dateDropDownElement;
    }

    protected loadComponentState(state: FilterState) {
        let startDateNano = formatDateAsNanosecondString(state.date?.startDate);
        let endDateNano = formatDateAsNanosecondString(state.date?.endDate);

        let startDateMilli = LvUtils.nano2milliseconds(startDateNano);
        let endDateMilli = LvUtils.nano2milliseconds(endDateNano);

        this.startDate = startDateMilli ? moment(startDateMilli) : null;
        this.endDate = endDateMilli ? moment(endDateMilli) : null;

        if (!this.startDate && !this.endDate) {
            this.title = this.languageService.getTranslate('DATE_INTERVAL.EMPTY_TIMESTAMP_FILTER');
        } else if (this.startDate && this.endDate) {
            this.title = LvDateIntervalComponent.niceFormat(startDateMilli) + ' - ' + LvDateIntervalComponent.niceFormat(endDateMilli);
        } else {
            if (this.endDate) {
                const till = this.languageService.getTranslate('DATE_INTERVAL.TILL');

                this.title = `${till} ${LvDateIntervalComponent.niceFormat(endDateMilli)}`;
            } else {
                const since = this.languageService.getTranslate('DATE_INTERVAL.SINCE');

                this.title = `${since} ${LvDateIntervalComponent.niceFormat(startDateMilli)}`;
            }
        }
    }

    private static niceFormat(date: number) {
        let d = moment(date);

        if ((date % 1000) !== 0) {
            return d.format('YYYY-MM-DD HH:mm:ss.SSS');
        } else if ((date % 60000) !== 0) {
            return d.format('YYYY-MM-DD HH:mm:ss');
        } else if ((date % (24 * 60 * 60 * 1000)) !== 0) {
            return d.format('YYYY-MM-DD HH:mm');
        } else {
            return d.format('YYYY-MM-DD');
        }
    }

    onApply() {
        if (this.startDate && this.endDate && (this.startDate > this.endDate)) {
            return false;
        }

        this.filterPanelStateService.updateFilterState(state => {
            state.date = {
                startDate: this.startDate ? LvUtils.milliseconds2nano(this.startDate.toDate().getTime()) : undefined,
                endDate: this.endDate ? LvUtils.milliseconds2nano(this.endDate.toDate().getTime()) : undefined,
            };
        });

        this.dropdownShown = false;
    }

    onCancel() {
        this.dropdownShown = false;
        this.loadComponentState(this.filterPanelStateService.getFilterState());
    }

    toggleFilterPanel() {
        super.toggleFilterPanel();
        this.calculateDefaultDate();
    }

    private calculateDefaultDate() {
        this.defaultDate = null;

        if (this.filterPanelStateService.currentRecords) {
            for (let record of this.filterPanelStateService.currentRecords) {
                if (record.time) {
                    this.defaultDate = moment(LvUtils.nano2milliseconds(record.time));
                    break;
                }
            }
        }
    }

    removeFilter() {
        this.filterPanelStateService.updateFilterState(state => {
            state.date = undefined;
        });
    }

    lastXMin(minutes: number) {
        this.startDate = moment().subtract(minutes, 'm').set({second: 0, millisecond: 0});
        this.endDate = null;
        this.onApply();
    }

    today() {
        this.startDate = moment().set({hour: 0, minute: 0, second: 0, millisecond: 0});
        this.endDate = null;
        this.onApply();
    }

    yesterday() {
        this.startDate = moment().subtract(1, 'd').set({hour: 0, minute: 0, second: 0, millisecond: 0});
        this.endDate = this.startDate.clone().add(1, 'd');
        this.onApply();
    }

    sinceCurrentMoment() {
        this.startDate = moment().set({millisecond: 0});
        this.endDate = null;
        this.onApply();
    }

    clearEndDate() {
        this.endDate = null;
    }

    clearStartDate() {
        this.startDate = null;
    }
}
