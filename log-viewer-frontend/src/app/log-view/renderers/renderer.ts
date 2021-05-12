import {Record} from '@app/log-view/record';

export type Text2HtmlConverter = (e: HTMLElement,
                                  type: string,
                                  s: string) => void;

export interface Highlight {
    start: number;
    end: number;
    e: HTMLElement;
}

export interface RenderContext {
    compact: boolean;
    textRenderer: Text2HtmlConverter;
}

export interface FieldRenderer {
    append(e: HTMLElement,
           s: string,
           record: Record,
           rendererCtx: RenderContext,
    ): void;
}

export interface TextRenderer {
    tryRender(s: string, textRenderer?: Text2HtmlConverter): Highlight[];
}
