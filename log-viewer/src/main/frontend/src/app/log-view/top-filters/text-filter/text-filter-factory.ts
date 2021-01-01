import {FilterFactory, FilterState} from '@app/log-view/filter-panel-state.service';
import {NotPredicate, Predicate, SubstringPredicate} from '@app/log-view/predicates';
import * as equal from 'fast-deep-equal';

export class TextFilterFactory implements FilterFactory {
    addFilters(res: Predicate[], state: FilterState): void {
        if (state.textFilters) {
            for (let f of state.textFilters) {
                if (f.pattern?.s) {
                    let p: Predicate = <SubstringPredicate>{type: 'SubstringPredicate', search: Object.assign({}, f.pattern)};

                    if (f.exclude) {
                        p = <NotPredicate>{type: 'NotPredicate', delegate: p};
                    }

                    res.push(p);
                }
            }
        }
    }

    compareFilterState(state1: FilterState, state2: FilterState): boolean {
        return equal(state1.textFilters ?? [], state2.textFilters ?? []);
    }
}
