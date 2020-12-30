import {FilterFactory, FilterState} from '@app/log-view/filter-panel-state.service';
import {GroovyPredicate, Predicate} from '@app/log-view/predicates';
import * as equal from 'fast-deep-equal';

export class GroovyFilterFactory implements FilterFactory {
    addFilters(res: Predicate[], state: FilterState): void {
        if (state.groovyFilters) {
            for (let f of state.groovyFilters) {
                res.push(<GroovyPredicate>{type: 'GroovyPredicate', script: f.script});
            }
        }
    }

    compareFilterState(state1: FilterState, state2: FilterState): boolean {
        return equal(state1.groovyFilters ?? [], state2.groovyFilters ?? []);
    }
}
