import {Record} from './record';
import {LvUtils} from '@app/utils/utils';
import {LogFile} from '@app/log-view/log-file';
import {Injectable} from '@angular/core';
import {FilterPanelStateService} from '@app/log-view/filter-panel-state.service';
import * as $ from 'jquery';

@Injectable()
export class ContextMenuHandler {

    constructor(private filterPanelStateService: FilterPanelStateService) {
    }

    isTextSelected(): boolean {
        return !getSelection().isCollapsed;
    }

    isThreadMenuItemVisible(item: Item): boolean {
        return !!item.thread;
    }

    isTextMenuItemVisible(item: Item): boolean {
        return !!item.selectedText;
    }

    isDateMenuItemVisible(item: Item): boolean {
        return !!item.record.time;
    }

    hasThreadGroup(item: Item): boolean {
        return !!item.threadGroup;
    }

    createItem(record: Record, logs: LogFile[]): Item {
        let res: Item = {record};

        let thread = LvUtils.fieldValueByType(record, logs, 'thread');
        if (thread) {
            res.thread = thread;
            
            let matchResult = thread.match(/^(.+)\b(\d+)$/);
            if (matchResult) {
                res.threadGroup = matchResult[1] + '*';
                res.threadGroupHtml = LvUtils.escapeHtml(matchResult[1]) + '<strong>*</strong>';
            }
        }

        let selectedText = ContextMenuHandler.getSelectedText();

        if (selectedText) {
            res.selectedText = selectedText;
            res.selectedTextVisible = LvUtils.trimText(res.selectedText, 30);
        }

        return res;
    }

    private static getSelectedText(): string {
        let rangeCount = document.getSelection().rangeCount;
        if (!rangeCount) {
            return null;
        }

        let selectedTextContainer = document.createElement('DIV');

        let record = null;

        for (let i = 0; i < rangeCount; i++) {
            let range = document.getSelection().getRangeAt(i);

            let rangeStart = ContextMenuHandler.parentRecordElement(range.startContainer);
            let rangeEnd = ContextMenuHandler.parentRecordElement(range.endContainer);
            if (rangeStart !== rangeEnd || !rangeStart) {
                return null;
            }

            if (record == null) {
                record = rangeStart;
            } else if (record !== rangeStart) {
                return null;
            }

            selectedTextContainer.append(range.cloneContents());
        }

        $('.lv-virtual', selectedTextContainer).remove();

        return selectedTextContainer.innerText;
    }

    private static parentRecordElement(e: Node): Element {
        while (true) {
            if (!e) {
                return null;
            }

            if ((<Element>e).tagName === 'DIV') {
                let classList = (<Element>e).classList;
                if (classList && classList.contains('record')) {
                    return <Element>e;
                }
            }

            e = e.parentElement;
        }
    }

    copyToClipboard(): void {
        document.execCommand('copy')
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

        LvUtils.highlight('.search-bar lv-date-interval .interval-title');
    }

    excludeThread(thread: string) {
        this.filterPanelStateService.updateFilterState(state => {
            if (!state.thread) {
                state.thread = {};
            }

            if (!state.thread.excludes) {
                state.thread.excludes = [];
            }

            LvUtils.addIfNotExist(state.thread.excludes, thread);
        });

        ContextMenuHandler.highlightThreadFilter();
    }

    filterByThread(thread: string) {
        this.filterPanelStateService.updateFilterState(state => {
            state.thread = {includes: [thread]};
        });

        ContextMenuHandler.highlightThreadFilter();
    }

    private static highlightThreadFilter() {
        LvUtils.highlight('.search-bar lv-thread-filter .top-panel-dropdown > span');
    }

    filterByText(text: string, exclude: boolean) {
        let id = this.filterPanelStateService.generateRandomId();

        this.filterPanelStateService.updateFilterState(state => {
            if (!state.textFilters) {
                state.textFilters = [];
            }

            state.textFilters.push({
                id,
                pattern: {
                    s: text,
                },
                exclude,
            });
        });

        LvUtils.highlight('.search-bar lv-text-filter .top-panel-dropdown[filter-id="' + id + '"]');
    }
}

export interface Item {
    record: Record;
    thread?: string;
    threadGroup?: string;
    threadGroupHtml?: string;
    selectedText?: string;
    selectedTextVisible?: string;
}
