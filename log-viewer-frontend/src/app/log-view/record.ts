import {Marker} from '../utils/marker';
import {Position} from './position';

export class Record {
    logId: string;

    start: number;
    end: number;

    time: number;

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

    static compareTo(a: Record, b: Record) {
        let res = a.time - b.time;
        if (res !== 0) { return res; }

        res = a.logId.localeCompare(b.logId);
        if (res !== 0) { return res; }

        return a.start - b.start;
    }
}
