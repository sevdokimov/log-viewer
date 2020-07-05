import {Record} from './record';

export class Position {
    constructor(public logId: string, public time: number, public o: number) {
    }

    // equals(p: Position): boolean {
    //     return this.logId === p.logId && this.time == p.time && this.o === p.o
    // }

     static recordStart(record: Record): Position {
        return new Position(record.logId, record.time, record.start);
    }

    static recordEnd(record: Record): Position {
        return new Position(record.logId, record.time, record.end);
    }
}
