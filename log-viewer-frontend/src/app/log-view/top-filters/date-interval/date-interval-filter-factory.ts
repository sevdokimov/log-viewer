import {FilterFactory, FilterState} from '@app/log-view/filter-panel-state.service';
import {DatePredicate, Predicate} from '@app/log-view/predicates';

export class DateIntervalFilterFactory implements FilterFactory {
    addFilters(res: Predicate[], state: FilterState): void {
        if (state.date?.startDate) {
            res.push(<DatePredicate>{type: 'DatePredicate', timestamp: state.date.startDate, greater: true});
        }
        if (state.date?.endDate) {
            res.push(<DatePredicate>{type: 'DatePredicate', timestamp: state.date.endDate, greater: false});
        }
    }

    compareFilterState(state1: FilterState, state2: FilterState): boolean {
        return state1.date?.startDate === state2.date?.startDate && state1.date?.endDate === state2.date?.endDate;
    }
}
