import {Component, ElementRef, ViewChild} from '@angular/core';
import {FilterPanelStateService, FilterState} from '@app/log-view/filter-panel-state.service';
import {FilterEditorComponent} from '@app/log-view/top-filters/filter-editor-component';
import {NGX_MAT_DATE_FORMATS, NgxMatDateFormats} from '@angular-material-components/datetime-picker';
import {Moment} from 'moment/moment';
import * as moment from 'moment';

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
        { provide: NGX_MAT_DATE_FORMATS, useValue: CUSTOM_DATE_FORMATS }
    ]
})
export class LvDateIntervalComponent extends FilterEditorComponent {

    @ViewChild('dateDropDown', {static: true})
    private dateDropDownElement: ElementRef;

    title: string;

    endDate: Moment;
    startDate: Moment;

    dropdownShown: boolean;
    dropdownRight: boolean;

    constructor(filterPanelStateService: FilterPanelStateService) {
        super(filterPanelStateService);
    }

    protected loadComponentState(state: FilterState) {
        this.startDate = state.startDate ? moment(state.startDate) : null;
        this.endDate = state.endDate ? moment(state.endDate) : null;

        if (!state.startDate && !state.endDate) {
            this.title = 'No time restriction';
        } else if (state.startDate && state.endDate) {
            this.title = LvDateIntervalComponent.niceFormat(state.startDate) + ' - ' + LvDateIntervalComponent.niceFormat(state.endDate);
        } else {
            if (state.endDate) {
                this.title = 'Till ' + LvDateIntervalComponent.niceFormat(state.endDate);
            } else {
                this.title = 'Since ' + LvDateIntervalComponent.niceFormat(state.startDate);
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
            state.startDate = this.startDate ? this.startDate.toDate().getTime() : null;
            state.endDate = this.endDate ? this.endDate.toDate().getTime() : null;
        });

        this.dropdownShown = false;
    }

    onCancel() {
        this.dropdownShown = false;
        this.loadComponentState(this.filterPanelStateService.getFilterState());
    }

    toggleFilterPanel() {
        if (this.dropdownShown) {
            this.dropdownShown = false;
        } else {
            this.dropdownShown = true;

            let rect = this.dateDropDownElement.nativeElement.getBoundingClientRect();
            this.dropdownRight = rect.x + rect.width > 440;
        }
    }

    clear() {
        this.startDate = null;
        this.endDate = null;
        this.onApply();
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
