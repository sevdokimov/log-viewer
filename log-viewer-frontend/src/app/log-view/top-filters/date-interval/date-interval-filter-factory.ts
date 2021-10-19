import {FilterFactory, FilterState} from '@app/log-view/filter-panel-state.service';
import {DatePredicate, Predicate} from '@app/log-view/predicates';
import {LvUtils} from "@app/utils/utils";

export class DateIntervalFilterFactory implements FilterFactory {
    addFilters(res: Predicate[], state: FilterState): void {
        let startDate = formatDateAsNanosecondString(state.date?.startDate)
        if (startDate) {
            res.push(<DatePredicate>{type: 'DatePredicate', timestamp: startDate, greater: true});
        }

        let endDate = formatDateAsNanosecondString(state.date?.endDate)
        if (endDate) {
            res.push(<DatePredicate>{type: 'DatePredicate', timestamp: endDate, greater: false});
        }
    }
    compareFilterState(state1: FilterState, state2: FilterState): boolean {
        return formatDateAsNanosecondString(state1.date?.startDate) === formatDateAsNanosecondString(state2.date?.startDate)
                && formatDateAsNanosecondString(state1.date?.endDate) === formatDateAsNanosecondString(state2.date?.endDate)
    }
}

export function formatDateAsNanosecondString(anyFormatDate: any): string {
    if (!anyFormatDate)
        return null;

    if (typeof anyFormatDate === 'number') // fix filter states saved by old versions
        return LvUtils.milliseconds2nano(anyFormatDate)

    if (typeof anyFormatDate !== 'string') {
        console.error('Invalid date: ', anyFormatDate);
        return null;
    }

    return anyFormatDate;
}