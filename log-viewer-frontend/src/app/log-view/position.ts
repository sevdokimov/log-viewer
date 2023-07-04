import {Record} from './record';

export class Position {
    constructor(public logId: string, public time: string, public o: number) {
    }

    static equals(p1: Position, p2: Position): boolean {
        if (!p1 && !p2) { return true; }
        if (!p1 || !p2) { return false; }
            
        // tslint:disable-next-line:triple-equals
        return p1.logId === p2.logId && p1.time == p2.time && p1.o === p2.o;
    }

     static recordStart(record: Record): Position {
        return new Position(record.logId, record.time, record.start);
    }

    static recordEnd(record: Record): Position {
        return new Position(record.logId, record.time, record.end);
    }

    static firstLine(): Position {
        let dateInPastNano = '100000000000000' // 1970-01-02 03:46:40
        return  new Position('', dateInPastNano, 0); // a date in distant past
    }

    static containPosition(position: Position, record: Record): boolean {
        if (!position) { return false; }

        return (
            position.logId === record.logId &&
            position.time === record.time &&
            (record.start <= position.o && position.o <= record.end)
        );
    }
}
