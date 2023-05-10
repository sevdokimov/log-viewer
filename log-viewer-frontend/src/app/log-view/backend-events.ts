import {LogFile, RestStatus} from './log-file';
import {FieldTypeDescription} from './view-config.service';
import {Record} from './record';
import {Position} from './position';
import {SearchPattern} from './search';
import {LogList} from '@app/utils/log-path-utills';

export interface BackendEvent {
    name: string;
}

export interface BackendErrorEvent extends BackendEvent {
    stacktrace: string;
}

export interface SetFilterStateEvent extends BackendEvent {
    urlParamValue?: string;
    filterState: string;
}

export interface EventSetViewState extends BackendEvent {
    logs: LogFile[];

    uiConfig: string;

    inFavorites: boolean;
    favEditable: boolean;

    globalSavedFilters: { [key: string]: string };

    localhostName: string;

    initByPermalink: boolean;
}

export interface UiConfig {
    'field-types': { [key: string]: FieldTypeDescription };

    'text-highlighters': { [key: string]: TextHighlighterConfig };

    properties: { [key: string]: any };

    'send-usage-statistics': boolean;
}

export class UiConfigValidator {
    static validateUiConfig(ui: UiConfig): string {
        for (let [name, h] of Object.entries(ui['text-highlighters'])) {
            let error = UiConfigValidator.validateTextHighlighter(name, h);
            if (error) { return error; }
        }
    }

    static validateTextHighlighter(name: string, h: TextHighlighterConfig): string {
        if (h.priority && typeof h.priority !== 'number') {
            return 'Invalid configuration:<br>"<b>text-highlighters.' + name + '.priority</b>" must be a number';
        }
    }
}

export interface TextHighlighterConfig {
    'text-type': string[];
    class: string;
    args?: any;
    enabled?: boolean;
    priority?: number;
}

export interface StatusHolderEvent extends BackendEvent {
    statuses: { [key: string]: RestStatus };
    stateVersion: number;
}

export interface RecordBundle {
    records: Record[];
    hasNextLine: boolean;
}

export interface EventNextDataLoaded extends DataHolderEvent {
    start: Position;
    backward: boolean;
}

export interface EventScrollToEdgeResponse extends DataHolderEvent {
    isScrollToBegin: boolean;
}

export interface EventResponseAfterFilterChangedSingle
    extends DataHolderEvent {
}

export interface EventResponseAfterFilterChanged extends StatusHolderEvent {
    topData: RecordBundle;
    bottomData: RecordBundle;
}

export interface EventSearchResponse extends StatusHolderEvent {
    records: Record[];
    foundIdx: number;
    hasSkippedLine: boolean;
    requestId: number;
    hasNextLine: boolean;
}

export interface LoadLogContentResponse extends BackendEvent {
    logId: string;

    text: string;
    textLengthBytes: number;

    recordStart: number;
    offset: number;
}

export interface DataHolderEvent extends StatusHolderEvent {
    data: RecordBundle;
}

export interface EventInitByPermalink extends DataHolderEvent {
    logListQueryParams: any;
    logList: LogList;
    selectedLine: Position;
    shiftView: number;
    searchPattern: SearchPattern;
    hideUnmatched: boolean;
    savedFilterName: string;
    filterStateUrlParam: string;
}

export interface FileAttributes {
    size: number;
    modifiedTime: number;
}


export interface EventsLogChanged extends BackendEvent {
    changedLogs: { [key: string]: FileAttributes };
}
