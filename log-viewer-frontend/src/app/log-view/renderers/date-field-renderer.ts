import {FieldRenderer} from '@app/log-view/renderers/renderer';
import {SlStyle} from '@app/log-view/renderers/style';
import {LvUtils} from '@app/utils/utils';
import {Record} from '@app/log-view/record';

export class DateFieldRenderer implements FieldRenderer {

    private style: SlStyle;

    constructor(private cfg: {style?: SlStyle}) {
        this.style = (!cfg || !cfg.style) ? {color: '#009'} : cfg.style;
    }

    append(e: HTMLElement, s: string, record: Record) {
        let span = document.createElement('SPAN');
        LvUtils.applyStyle(span, this.style);
        span.textContent = s;

        if (record.time) {
            span.title = LvUtils.formatDate(Record.nano2milliseconds(record.time));
        }

        e.appendChild(span);
    }
}
