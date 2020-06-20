import {FilterFactory, FilterState} from '@app/log-view/filter-panel-state.service';
import {ExceptionOnlyPredicate, NotPredicate, Predicate} from '@app/log-view/predicates';

export class ExceptionOnlyFilterFactory implements FilterFactory {
    addFilters(res: Predicate[], state: FilterState): void {
        if (state.exceptionsOnly) {
            let exFilter: ExceptionOnlyPredicate = {type: 'ExceptionOnlyPredicate'};

            res.push(<NotPredicate>{type: 'NotPredicate', delegate: exFilter});
        }
    }

    compareFilterState(state1: FilterState, state2: FilterState): boolean {
        return !!state1.exceptionsOnly === !!state2.exceptionsOnly;
    }

}