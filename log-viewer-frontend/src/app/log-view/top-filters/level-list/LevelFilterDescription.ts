import {FilterFactory, FilterState} from '@app/log-view/filter-panel-state.service';
import {FieldValueSetPredicate, NotPredicate, Predicate} from '@app/log-view/predicates';

export class LevelFilterDescription implements FilterFactory {

    private static levelGroups = [
        ['ERROR', 'SEVERE', 'FATAL'],
        ['WARN', 'WARNING'],
        ['INFO'],
        ['DEBUG', 'CONFIG'],
        ['TRACE', 'FINE', 'FINER', 'FINEST'],
    ];

    private synonyms: {[key: string]: string[]} = {};

    constructor(public levels: string[],
                public fieldType: string,
                ) {
        for (let l of levels) {
            let group = LevelFilterDescription.levelGroups.find(g => g.includes(l)) || [];
            this.synonyms[l] = group.filter(s => !levels.includes(s));
        }
    }

    addFilters(res: Predicate[], state: FilterState): void {
        if (!state.level) {
            return;
        }

        let excludedLevels = this.levels.filter(level => !state.level.includes(level));

        if (excludedLevels.length === this.levels.length) {
            return;
        }

        let excludedLevelsWithSynonyms = [...excludedLevels];
        for (let l of excludedLevels) {
            excludedLevelsWithSynonyms.push(...(this.synonyms[l] || []));
        }

        let exclude = <FieldValueSetPredicate>{type: 'FieldValueSetPredicate', fieldType: this.fieldType,
            values: excludedLevelsWithSynonyms};

        res.push(<NotPredicate>{type: 'NotPredicate', delegate: exclude});
    }

    compareFilterState(state1: FilterState, state2: FilterState): boolean {
        let levels1 = state1.level || [];
        let levels2 = state2.level || [];

        if (this.levels.findIndex(level => !levels1.includes(level)) < 0) { // all levels are enabled
            levels1 = [];
        }

        if (this.levels.findIndex(level => !levels2.includes(level)) < 0) { // all levels are enabled
            levels2 = [];
        }

        for (let level of this.levels) {
            if (levels1.includes(level) !== levels2.includes(level)) {
                return false;
            }
        }

        return true;
    }
}
