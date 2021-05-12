import {FilterFactory, FilterState} from '@app/log-view/filter-panel-state.service';
import {ExceptionOnlyPredicate, Predicate} from '@app/log-view/predicates';

export class ExceptionOnlyFilterFactory implements FilterFactory {
    addFilters(res: Predicate[], state: FilterState): void {
        if (state.exceptionsOnly) {
            res.push({type: 'ExceptionOnlyPredicate'});
        }
    }

    compareFilterState(state1: FilterState, state2: FilterState): boolean {
        return !!state1.exceptionsOnly === !!state2.exceptionsOnly;
    }

}
