import {FieldRenderer, RenderContext} from './renderer';
import {LvUtils} from '@app/utils/utils';
import {SlStyle} from './style';
import {Record} from '@app/log-view/record';

export class TextFieldRenderer implements FieldRenderer {
    private style: SlStyle;

    private textType: string;

    constructor(cfg: TextFieldRendererCfg = {}) {
        this.style = cfg.style;
        this.textType = cfg.textType || 'text';
    }

    append(e: HTMLElement, s: string, record: Record, rendererCtx: RenderContext) {
        if (this.style) {
            let span = document.createElement('SPAN');
            LvUtils.applyStyle(span, this.style);
            rendererCtx.textRenderer(span, this.textType, s);
            e.appendChild(span);
        } else {
            rendererCtx.textRenderer(e, this.textType, s);
        }
    }
}

export interface TextFieldRendererCfg {
    style?: SlStyle;

    textType?: string;
}
