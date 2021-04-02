import {FilterFactory, FilterState} from '@app/log-view/filter-panel-state.service';
import {Predicate, ThreadPredicate} from '@app/log-view/predicates';
import * as equal from 'fast-deep-equal';

export class ThreadFilterFactory implements FilterFactory {
    addFilters(res: Predicate[], state: FilterState): void {
        if (state.thread && (state.thread.includes?.length || state.thread.excludes?.length)) {
            res.push(<ThreadPredicate>{type: 'ThreadPredicate', includes: state.thread.includes, excludes: state.thread.excludes});
        }
    }

    compareFilterState(state1: FilterState, state2: FilterState): boolean {
        return equal(state1.thread?.includes ?? [], state2.thread?.includes ?? [])
            && equal(state1.thread?.excludes ?? [], state2.thread?.excludes ?? []);
    }
}
