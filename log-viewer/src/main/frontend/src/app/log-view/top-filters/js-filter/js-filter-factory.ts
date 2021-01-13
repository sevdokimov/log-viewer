import {FilterFactory, FilterState} from '@app/log-view/filter-panel-state.service';
import {JsPredicate, Predicate} from '@app/log-view/predicates';
import * as equal from 'fast-deep-equal';

export class JsFilterFactory implements FilterFactory {
    addFilters(res: Predicate[], state: FilterState): void {
        if (state.jsFilters) {
            for (let f of state.jsFilters) {
                res.push(<JsPredicate>{type: 'JsPredicate', script: f.script});
            }
        }
    }

    compareFilterState(state1: FilterState, state2: FilterState): boolean {
        return equal(state1.jsFilters ?? [], state2.jsFilters ?? []);
    }
}
