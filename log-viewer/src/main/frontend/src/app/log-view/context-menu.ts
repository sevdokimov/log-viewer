import {Record} from './record';
import {SlUtils} from '@app/utils/utils';
import {LogFile} from '@app/log-view/log-file';
import {Injectable} from '@angular/core';
import * as $ from 'jquery';
import {FilterPanelStateService} from '@app/log-view/filter-panel-state.service';

@Injectable()
export class ContextMenuHandler {

    constructor(private filterPanelStateService: FilterPanelStateService) {
    }

    isThreadMenuItemVisible(item: Item) {
        return item.thread;
    }

    isDateMenuItemVisible(item: Item) {
        return item.record.time;
    }

    hasThreadGroup(item: Item) {
        return item.threadGroup;
    }

    createItem(record: Record, logs: LogFile[]): Item {
        let res: Item = {record};

        let thread = SlUtils.fieldValueByType(record, logs, 'thread');
        let threadGroup = null;
        if (thread) {
            res.thread = thread;
            
            let matchResult = thread.match(/^(.+)\b(\d+)$/);
            if (matchResult) {
                res.threadGroup = matchResult[1] + '*';
                res.threadGroupHtml = SlUtils.escapeHtml(matchResult[1]) + '<strong>*</strong>';
            }
        }

        return res;
    }

    hideEventsByTimestamp(record: Record, next: boolean) {
        if (!record.time) {
            return;
        }

        this.filterPanelStateService.updateFilterState(state => {
            if (!state.date) {
                state.date = {};
            }
            if (next) {
                state.date.endDate = record.time;
            } else {
                state.date.startDate = record.time;
            }
        });

        SlUtils.highlight($('.search-bar lv-date-interval .interval-title')[0]);
    }

    excludeThread(thread: string) {
        this.filterPanelStateService.updateFilterState(state => {
            if (!state.thread) {
                state.thread = {};
            }

            if (!state.thread.excludes) {
                state.thread.excludes = [];
            }

            SlUtils.addIfNotExist(state.thread.excludes, thread);
        });
    }

    filterByThread(thread: string) {
        this.filterPanelStateService.updateFilterState(state => {
            state.thread = {includes: [thread]};
        });
    }
}

export interface Item {
    record: Record;
    thread?: string;
    threadGroup?: string;
    threadGroupHtml?: string;
}
