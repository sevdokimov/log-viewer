import {EventEmitter, Injectable} from '@angular/core';
import {Predicate} from '@app/log-view/predicates';
import {LogFile} from '@app/log-view/log-file';
import {Md5} from 'ts-md5';
import {HttpClient} from '@angular/common/http';
import {LevelFilterDescription} from '@app/log-view/top-filters/level-list/LevelFilterDescription';
import * as equal from 'fast-deep-equal';
import {ExceptionOnlyFilterFactory} from '@app/log-view/top-filters/exception-only/exception-only-filter-factory';

@Injectable()
export class FilterPanelStateService {

    private _state: FilterState = {};

    private _stateStr: string;
    get stateStr() {
        return this._stateStr;
    }

    private _stateHash: string;
    get stateHash() {
        return this._stateHash;
    }

    activeFilterEditors: {[key: string]: FilterFactory} = {};

    filterChanges = new EventEmitter<FilterState>();

    private updating: boolean;

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

    init(logs: LogFile[], filterState: FilterState) {
        this._state = filterState;

        this._stateStr = JSON.stringify(this._state);
        this._stateHash = Md5.hashStr(this._stateStr).toString();

        let levelType = FilterPanelStateService.findCommonLevelType(logs);

        if (levelType === 'level/log4j') {
            this.activeFilterEditors.level = new LevelFilterDescription(['FATAL', 'ERROR', 'WARN', 'INFO', 'DEBUG', 'TRACE'],
                'level');
        } else if (levelType) {
            // level/logback or any other log level

            this.activeFilterEditors.level = new LevelFilterDescription(['ERROR', 'WARN', 'INFO', 'DEBUG', 'TRACE'],
                'level');
        }

        this.activeFilterEditors.namedFilters = new NamedFilterFilterFactory();

        this.activeFilterEditors.exceptionOnly = new ExceptionOnlyFilterFactory();

        this.filterChanges.emit(this._state);
    }

    getFilterState(): FilterState {
        return this._state;
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
                this._stateStr = newStateStr;
                this._stateHash = Md5.hashStr(newStateStr).toString();
                
                this.http.post('rest/log-view/saveFilterState', [this._stateHash, newStateStr]).subscribe();

                this.filterChanges.emit(this._state);
            }
        } finally {
            this.updating = false;
        }
    }

    getActiveFilters(): Predicate[] {
        let res: Predicate[] = [];

        for (const  factory of Object.values(this.activeFilterEditors)) {
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

        return true;
    }
}

export interface FilterFactory {
    addFilters(res: Predicate[], state: FilterState): void;

    compareFilterState(state1: FilterState, state2: FilterState): boolean;
}

export interface FilterState {
    level?: string[];

    exceptionsOnly?: boolean;

    namedFilters?: Filter[];
}

export interface Filter {
    name?: string;
    enabled?: boolean;
    predicate: Predicate;
}

class NamedFilterFilterFactory implements FilterFactory {
    addFilters(res: Predicate[], state: FilterState): void {
        if (state.namedFilters) {
            for (let f of state.namedFilters) {
                if (f.enabled && f.predicate) {
                    res.push(f.predicate);
                }
            }
        }
    }

    compareFilterState(state1: FilterState, state2: FilterState): boolean {
        return equal(state1.namedFilters || [], state2.namedFilters || []);
    }
}
