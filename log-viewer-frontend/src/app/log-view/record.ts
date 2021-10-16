import {Marker} from '../utils/marker';

export class Record {
    logId: string;

    start: number;
    end: number;

    time: string; // nanoseconds in string representation like "01623601564799000000"

    hasMore: boolean;

    s: string;

    fields: Field[];

    filteringError: string;

    searchRes?: Marker[];
}

export interface Field {
    name: string;
    start: number;
    end: number;
}
