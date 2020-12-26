import {FilterFactory, FilterState} from '@app/log-view/filter-panel-state.service';
import {DatePredicate, Predicate} from '@app/log-view/predicates';

export class DateIntervalFilterFactory implements FilterFactory {
    addFilters(res: Predicate[], state: FilterState): void {
        if (state.startDate) {
            res.push(<DatePredicate>{type: 'DatePredicate', date: state.startDate, greater: true});
        }
        if (state.endDate) {
            res.push(<DatePredicate>{type: 'DatePredicate', date: state.endDate, greater: false});
        }
    }

    compareFilterState(state1: FilterState, state2: FilterState): boolean {
        return state1.startDate === state2.startDate && state1.endDate === state2.endDate;
    }
}
