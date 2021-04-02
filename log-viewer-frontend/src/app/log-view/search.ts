import {Record} from './record';
import {Marker} from '../utils/marker';

export class SearchUtils {
    public static doSimpleSearch(records: Record[],
                                 pattern: SearchPattern,
                                 changed?: number[]) {
        for (let j = 0; j < records.length; j++) {
            let r = records[j];
            let markers: Marker[] = null;

            if (pattern) {
                if (pattern.regex) {
                    let regex = new RegExp(pattern.s, pattern.matchCase ? 'g' : 'ig');

                    let m;
                    while ((m = regex.exec(r.s))) {
                        if (m.length === 0 || m[0].length === 0) { continue; }

                        if (markers === null) { markers = []; }

                        markers.push({
                            start: m.index,
                            end: m.index + m[0].length,
                            className: 'search-result'
                        });
                    }
                } else {
                    let patternS = pattern.s;
                    let s = r.s;
                    if (!pattern.matchCase) {
                        s = s.toLowerCase();
                        patternS = patternS.toLowerCase();
                    }

                    let i = 0;
                    while (true) {
                        let idx = s.indexOf(patternS, i);
                        if (idx < 0) { break; }

                        if (markers === null) { markers = []; }

                        markers.push({
                            start: idx,
                            end: idx + patternS.length,
                            className: 'search-result'
                        });

                        i = idx + patternS.length;
                    }
                }
            }

            if (changed) {
                if (!Marker.equalsArray(r.searchRes, markers)) {
                    changed.push(j);
                }
            }

            r.searchRes = markers;
        }
    }

    static equals(p1: SearchPattern, p2: SearchPattern): boolean {
        if (p1 === p2) { return true; }

        if (!p1 || !p2) { return false; }

        return (
            (p1.s ?? '') === (p2.s ?? '') && !!p1.matchCase === !!p2.matchCase && !!p1.regex === !!p2.regex
        );
    }
}

export interface SearchPattern {
    s: string;
    matchCase?: boolean;
    regex?: boolean;
}
