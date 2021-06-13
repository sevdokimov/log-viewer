import {Marker} from '../utils/marker';
import {Position} from './position';
import {LvUtils} from '@app/utils/utils';

export class Record {
    logId: string;

    start: number;
    end: number;

    time: string; // nanoseconds in string representation like "01623601564799000000"

    hasMore: boolean;

    s: string;

    fieldsOffsetStart: number[];
    fieldsOffsetEnd: number[];

    filteringError: string;

    searchRes?: Marker[];

    static containPosition(position: Position, record: Record): boolean {
        if (!position) { return false; }

        return (
            position.logId === record.logId &&
            position.time === record.time &&
            (record.start <= position.o && position.o <= record.end)
        );
    }

    static equals(a: Record, b: Record) {
        return a.logId === b.logId && a.start === b.start;
    }

    static nano2milliseconds(nanoTimestamp: string): number {
        if (!nanoTimestamp) {
            return null;
        }

        LvUtils.assert(nanoTimestamp.length > 9, nanoTimestamp);
        return parseInt(nanoTimestamp.substr(0, nanoTimestamp.length - 6), 10);
    }
}
