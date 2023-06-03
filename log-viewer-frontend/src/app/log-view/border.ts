/* tslint:disable:no-bitwise */
import {StatusHolderEvent} from '@app/log-view/backend-events';
import {Record} from './record';
import {LvUtils} from '@app/utils/utils';

const FLAG_EOF = 1 // see com.logviewer.web.session.Status.FLAG_EOF
const FLAG_LAST_RECORD_FILTER_TIME_LIMIT = 2 // see com.logviewer.web.session.Status.FLAG_LAST_RECORD_FILTER_TIME_LIMIT
const FLAG_LAST_RECORD_FILTER_MATCH = 4 // see com.logviewer.web.session.Status.FLAG_LAST_RECORD_FILTER_MATCH
const FLAG_FIRST_RECORD_FILTER_MATCH = 8 // see com.logviewer.web.session.Status.FLAG_FIRST_RECORD_FILTER_MATCH

export class Border {

    private logs: {[key: string]: {
            lastRecordOffset: number,
            flags: number,
        }} = {};

    hasNextLines: boolean

    constructor(event?: StatusHolderEvent) {
        if (!event) {
            this.hasNextLines = true
        } else {
            this.hasNextLines = false;

            for (let [logId, status] of Object.entries(event.statuses)) {
                if (status.errorType != null)
                    continue;

                if ((status.flags & FLAG_EOF) === 0) {
                    this.hasNextLines = true;
                }

                this.logs[logId] = {lastRecordOffset: status.lastRecordOffset, flags: status.flags};
            }
        }
    }

    onDeleteRecords(deleted: Record[], isBottomBorder: boolean) {
        for (let i = 0; i < deleted.length; i++) {
            let idx = i;
            if (isBottomBorder) {
                idx = deleted.length - idx - 1; // reverse the order of the iteration
            }

            let r = deleted[idx];

            let log = this.logs[r.logId]
            if (!log)
                continue;

            if (log.lastRecordOffset >= 0) {
                LvUtils.assert(isBottomBorder ? log.lastRecordOffset >= r.start : log.lastRecordOffset <= r.start)
            }

            log.lastRecordOffset = r.start;
            log.flags = FLAG_LAST_RECORD_FILTER_MATCH
        }
    }

    static createTopBorder(logIds: string[]): Border {
        let res = new Border();

        res.hasNextLines = false;

        for (let logId of logIds) {
            res.logs[logId] = {lastRecordOffset: 0, flags: FLAG_EOF};
        }

        return res;
    }

    static createBottomBorderBorder(event: StatusHolderEvent, eof: boolean): Border {
        let res = new Border();

        res.hasNextLines = !eof;

        for (let [logId, status] of Object.entries(event.statuses)) {
            if (status.errorType != null)
                continue;

            let flags = 0;

            if (status.flags & FLAG_FIRST_RECORD_FILTER_MATCH) {
                flags |= FLAG_LAST_RECORD_FILTER_MATCH;
            }

            if (eof) {
                flags |= FLAG_EOF;
            }

            res.logs[logId] = {lastRecordOffset: status.firstRecordOffset, flags};
        }

        return res;
    }
}
