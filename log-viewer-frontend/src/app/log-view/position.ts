import {Record} from './record';

export class Position {
    constructor(public logId: string, public time: string, public o: number) {
    }

    static equals(p1: Position, p2: Position): boolean {
        if (!p1 && !p2) { return true; }
        if (!p1 || !p2) { return false; }
            
        return p1.logId === p2.logId && p1.time === p2.time && p1.o === p2.o;
    }

     static recordStart(record: Record): Position {
        return new Position(record.logId, record.time, record.start);
    }

    static recordEnd(record: Record): Position {
        return new Position(record.logId, record.time, record.end);
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
