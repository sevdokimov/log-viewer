import {EventEmitter, Injectable} from '@angular/core';
import {Predicate} from '@app/log-view/predicates';
import {LogFile} from '@app/log-view/log-file';
import {Md5} from 'ts-md5';
import {HttpClient} from '@angular/common/http';
import {LevelFilterDescription} from '@app/log-view/top-filters/level-list/LevelFilterDescription';
import {ExceptionOnlyFilterFactory} from '@app/log-view/top-filters/exception-only/exception-only-filter-factory';
import {DateIntervalFilterFactory} from '@app/log-view/top-filters/date-interval/date-interval-filter-factory';
import {Record} from '@app/log-view/record';
import {ThreadFilterFactory} from '@app/log-view/top-filters/thread-filter/thread-filter-factory';
import {JsFilterFactory} from '@app/log-view/top-filters/js-filter/js-filter-factory';
import {FilterWithDropdown} from '@app/log-view/top-filters/filter-with-dropdown';
import * as $ from 'jquery';
import {SearchPattern} from '@app/log-view/search';
import {TextFilterFactory} from '@app/log-view/top-filters/text-filter/text-filter-factory';
import {UiConfig} from '@app/log-view/backend-events';
import {get} from 'lodash';

@Injectable()
export class FilterPanelStateService {

    private _state: FilterState = {};

    private _urlParamValue: string;
    get urlParamValue() {
        return this._urlParamValue;
    }

    uiConfig: UiConfig;

    activeFilterEditors: {[key: string]: FilterFactory} = {};

    permanentFilters: FilterFactory[] = [new TextFilterFactory(), new ExceptionOnlyFilterFactory(), new JsFilterFactory()];

    filterChanges = new EventEmitter<FilterState>();

    private updating: boolean;

    currentRecords: Record[];

    openedDropdown: FilterWithDropdown;

    constructor(private http: HttpClient) {
    }

    private static findCommonLevelType(logs: LogFile[]) {
        let type = null;

        for (let l of logs) {
            for (let f of l.fields) {
                if (!f.type) {
                    continue;
                }

                if (f.type === 'level') {
                    return 'level';
                }

                if (f.type.startsWith('level/')) {
                    if (type == null) {
                        type = f.type;
                    } else {
                        if (type !== f.type) {
                            return 'level';
                        }
                    }
                }
            }
        }

        return type;
    }

    init(logs: LogFile[], uiConfig: UiConfig) {
        this.uiConfig = uiConfig;
        let levelType = FilterPanelStateService.findCommonLevelType(logs);
        let uiLevels = get(uiConfig, 'field-types.level.args', {});

        if (levelType === 'level/log4j') {
            this.activeFilterEditors.level = new LevelFilterDescription(['FATAL', 'ERROR', 'WARN', 'INFO', 'DEBUG', 'TRACE'],
                'level', uiLevels);
        } else if (levelType) {
            // level/logback or any other log level
            this.activeFilterEditors.level = new LevelFilterDescription(['ERROR', 'WARN', 'INFO', 'DEBUG', 'TRACE'],
                'level', uiLevels);
        }

        if (!logs.find(l => l.connected && !l.hasFullDate)) {
            this.activeFilterEditors.dateRange = new DateIntervalFilterFactory();
        }

        if (logs.find(l => l.fields.find(f => f.type === 'thread'))) {
            this.activeFilterEditors.thread = new ThreadFilterFactory();
        }

        this.filterChanges.emit(this._state);
    }

    getFilterState(): FilterState {
        return this._state;
    }

    setFilterStateFromUrlValue(urlParamValue: string) {
        this.setFilterState(this.parseFilterState(urlParamValue), urlParamValue);
    }

    setFilterState(state: FilterState, urlParamValue: string) {
        if (this.updating) {
            throw 'Update already in progress';
        }

        this._state = JSON.parse(JSON.stringify(state ?? {}))
        this._urlParamValue = urlParamValue;

        this.filterChanges.emit(this._state);
    }

    updateFilterState(transition: (state: FilterState) => void) {
        if (this.updating) {
            throw 'Update already in progress';
        }

        this.updating = true;

        try {
            let originalState = JSON.stringify(this._state);

            try {
                transition(this._state);
            } catch (e) {
                this._state = JSON.parse(originalState);
                throw e;
            }

            let newStateStr = JSON.stringify(this._state);

            if (newStateStr !== originalState) {
                this._urlParamValue = Md5.hashStr(newStateStr).toString();

                this.http.post('rest/log-view/saveFilterState', [this._urlParamValue, newStateStr]).subscribe();

                this.filterChanges.emit(this._state);
            }
        } finally {
            this.updating = false;
        }
    }

    getActiveFilters(): Predicate[] {
        let res: Predicate[] = [];

        for (const factory of Object.values(this.activeFilterEditors)) {
            factory.addFilters(res, this._state);
        }

        for (const factory of this.permanentFilters) {
            factory.addFilters(res, this._state);
        }

        return res;
    }

    isStateEquals(other: FilterState): boolean {
        other = other || {};

        for (const factory of Object.values(this.activeFilterEditors)) {
            if (!factory.compareFilterState(this._state, other)) {
                return false;
            }
        }

        for (const factory of this.permanentFilters) {
            if (!factory.compareFilterState(this._state, other)) {
                return false;
            }
        }

        return true;
    }

    addDateFilter() {
        this.updateFilterState(state => {
            state.date = {};
        });

        setTimeout(() => $('lv-date-interval .closeable-filter > span')[0]?.click(), 0);

        return false;
    }

    addThreadFilter() {
        this.updateFilterState(state => {
            state.thread = {};
        });

        setTimeout(() => $('lv-thread-filter .closeable-filter > span')[0]?.click(), 0);

        return false;
    }

    addStacktraceFilter() {
        this.updateFilterState(state => {
            state.exceptionsOnly = true;
        });

        return false;
    }

    addJsFilter() {
        let id: string;

        this.updateFilterState(state => {
            if (!state.jsFilters) {
                state.jsFilters = [];
            }

            id = this.generateRandomId();

            state.jsFilters.push({
                id,
                script: 'function isVisibleEvent(text, fields) {\n' +
                    '    return text.length > 0 || fields.msg.includes(\'some substring\')\n' +
                    '}',
            });
        });

        setTimeout(() => $('lv-js-filter .closeable-filter[filter-id="' + id + '"] > span')[0]?.click(), 0);

        return false;
    }

    generateRandomId(): string {
        let array = new Uint32Array(1);
        window.crypto.getRandomValues(array);

        return '' + array[0];
    }

    addTextFilter() {
        let id: string;

        this.updateFilterState(state => {
            if (!state.textFilters) {
                state.textFilters = [];
            }

            id = this.generateRandomId();

            state.textFilters.push({
                id,
                pattern: {s: ''},
            });
        });

        setTimeout(() => $('lv-text-filter .closeable-filter[filter-id="' + id + '"] > span')[0]?.click(), 0);

        return false;
    }

    parseFilterState(filterStateJson: string): FilterState {
        if (!filterStateJson) { return null; }

        try {
            let filterState: FilterState = JSON.parse(filterStateJson);

            if (typeof filterState !== 'object') {
                console.error('Filter panel state is not an object: ' + filterStateJson);
                return null;
            }

            return filterState;
        } catch (e) {
            console.error('Failed to parse filter panel state', e);
            return null;
        }
    }
}

export interface FilterFactory {
    addFilters(res: Predicate[], state: FilterState): void;

    compareFilterState(state1: FilterState, state2: FilterState): boolean;
}

export interface FilterState {
    level?: string[];

    exceptionsOnly?: boolean;

    jsFilters?: JsFilter[];

    textFilters?: TextFilter[];

    date?: {
        startDate?: string;
        endDate?: string;
    };

    thread?: {includes?: string[], excludes?: string[]};
}

export interface JsFilter {
    id: string;

    name?: string;

    script: string;

    disabled?: boolean;
}

export interface TextFilter {
    id: string;

    name?: string;

    pattern: SearchPattern;
    exclude?: boolean;

    disabled?: boolean;
}
