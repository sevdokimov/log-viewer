import {Record} from './record';
import {SlUtils} from '../utils/utils';
import {Injectable} from '@angular/core';
import {FieldDescr, ViewConfigService} from './view-config.service';
import {ViewStateService} from './view-state.service';
import {SlElement} from '../utils/sl-element';
import {RenderContext} from '@app/log-view/renderers/renderer';
import * as $ from 'jquery';

@Injectable()
export class RecordRendererService {
    private logLabelWidth: number;

    constructor(private viewConfig: ViewConfigService,
                private vs: ViewStateService) {
    }

    renderRange(m: Record[], start: number, end: number, target: HTMLElement) {
        if (end === start) { return; }
        if (end < start) { throw 'end < start: ' + end + ' < ' + start; }
        SlUtils.assert(start >= 0, 'start < 0');
        SlUtils.assert(end <= m.length, 'end > m.length');

        if (target.children.length !== m.length - (end - start)) {
            throw 'Inconsistent array length: ' +
            target.children.length +
            ' != ' +
            (m.length - (end - start));
        }

        if (start > 0) { SlUtils.assert(Record.compareTo(m[start - 1], m[start]) < 0); }
        if (end < m.length) {
            SlUtils.assert(Record.compareTo(m[end - 1], m[end]) < 0);
        }

        this.initLabelWidthIfNeeded(target);

        let before =
            target.children.length === start ? null : target.children.item(start);

        for (let i = start; i < end; i++) {
            let rec = this.render(m[i]);

            this.addSearchResults(m[i], rec);
            this.expandSelected(m[i], rec);

            target.insertBefore(rec, before);
        }
    }

    static updateRecordView(r: Record, e: HTMLElement) {
        for (let item = e.firstChild; item != null;) {
            let next = item.nextSibling;

            if (
                (<HTMLElement>item).tagName === 'DIV' &&
                (<HTMLElement>item).classList.contains('filtering-error')
            ) {
                (<HTMLElement>item).remove();
                break;
            }

            item = next;
        }

        if (r.filteringError != null) {
            e.appendChild(
                RecordRendererService.createFilteringErrorIcon(r.filteringError)
            );
        }
    }

    private static createFilteringErrorIcon(errorText: string): HTMLElement {
        let errorIcon = document.createElement('DIV');
        errorIcon.className = 'filtering-error';
        errorIcon.title = 'Error when applying filter, click for more details';
        (<any>errorIcon).errorText = errorText;

        return errorIcon;
    }

    replaceRange(replacement: Record[],
                 m: Record[],
                 start: number,
                 end: number,
                 target: HTMLElement) {
        SlUtils.assert(target.children.length === m.length);
        SlUtils.assert(start <= end);

        this.initLabelWidthIfNeeded(target);

        let repI = 0;
        let count = end - start;

        while (true) {
            if (count === 0) {
                if (repI === replacement.length) { break; }

                m.splice(start, 0, replacement[repI]);
                let rec = this.render(m[start]);

                this.addSearchResults(m[start], rec);
                this.expandSelected(m[start], rec);

                target.insertBefore(
                    rec,
                    start >= target.children.length ? null : target.children.item(start)
                );

                repI++;
                start++;
            } else if (repI === replacement.length) {
                m.splice(start, count);
                for (let i = 0; i < count; i++) {
                    target.removeChild(target.children.item(start));
                }

                break;
            } else {
                let repStart = replacement[repI].start;
                let mStart = m[start].start;

                if (repStart < mStart) {
                    m.splice(start, 0, replacement[repI]);
                    let rec = this.render(m[start]);

                    this.addSearchResults(m[start], rec);
                    this.expandSelected(m[start], rec);

                    target.insertBefore(
                        rec,
                        start >= target.children.length ? null : target.children.item(start)
                    );

                    repI++;
                    start++;
                } else if (mStart < repStart) {
                    m.splice(start, 1);
                    count--;
                    target.removeChild(target.children.item(start));
                } else {
                    m[start] = replacement[repI];
                    RecordRendererService.updateRecordView(m[start], <HTMLElement>(
                        target.children.item(start)
                    ));
                    start++;
                    repI++;
                    count--;
                }
            }
        }
    }

    private addSearchResults(r: Record, e: HTMLElement) {
        if (r.searchRes) {
            for (let marker of r.searchRes) {
                this.highlight(e, marker.start, marker.end, 'search-result');
            }
        }
    }

    private textRenderer = (e: HTMLElement, type: string, s: string) => {
        let renderers = this.viewConfig.getTextRenderers();

        if (s.length === 0) {
            return;
        }

        let ranges: (string | HTMLElement)[] = [s];

        for (let renderer of renderers) {
            if (renderer.supportedTypes.find(t => RecordRendererService.startWith(type, t)) == null) {
                continue;
            }

            let nextRanges = [];

            let hasStr = false;

            for (let r of ranges) {
                if (typeof r === 'string') {
                    let splits = renderer.renderer.tryRender(r, this.textRenderer);

                    if (splits && splits.length > 0) {
                        let idx = 0;

                        for (let sp of splits) {
                            SlUtils.assert(sp.start <= sp.end && sp.end <= r.length);

                            if (sp.start > idx) {
                                nextRanges.push(r.substring(idx, sp.start));
                                hasStr = true;
                            } else {
                                SlUtils.assert(idx === sp.start);
                            }

                            let len = this.setStrLenAttribute(sp.e);
                            SlUtils.assert(len === sp.end - sp.start, "Source text length is not equals the rendered text length");

                            nextRanges.push(sp.e);
                            idx = sp.end;
                        }

                        if (idx < r.length) {
                            nextRanges.push(r.substring(idx));
                            hasStr = true;
                        }
                    } else {
                        nextRanges.push(r);
                        hasStr = true;
                    }
                } else {
                    nextRanges.push(r);
                }
            }

            ranges = nextRanges;

            if (!hasStr) { break; }
        }

        for (let r of ranges) {
            if (typeof r === 'string') {
                e.appendChild(document.createTextNode(r));
            } else {
                e.appendChild(r);
            }
        }
    };

    private render(r: Record): HTMLDivElement {
        let e: HTMLDivElement = <HTMLDivElement>document.createElement('DIV');
        e.className = 'rec-text';

        let recPointer = <HTMLDivElement>document.createElement('DIV');
        recPointer.className = 'rec-pointer';
        recPointer.title = 'Show event details (Right Mouse click)';

        e.append(recPointer);


        let log = this.viewConfig.logById[r.logId];
        SlUtils.assert(log != null, 'Unexpected logId: ' + r.logId);
        SlUtils.assert(log.fields.length === r.fieldsOffsetEnd.length, 'Unexpected field count in record');

        let s = r.s;

        if (r.fieldsOffsetEnd.findIndex(idx => idx >= 0) < 0) {
            // unparsable record
            this.textRenderer(e, 'text', s);
        } else {
            let fieldsCopy = [...log.fields];
            fieldsCopy.sort((a, b) => r.fieldsOffsetStart[a._index] - r.fieldsOffsetStart[b._index] );

            let i = 0;

            const rendererCtx: RenderContext = {textRenderer: this.textRenderer, compact: true};

            for (let field of fieldsCopy) {
                let fieldStart = r.fieldsOffsetStart[field._index];
                let fieldEnd = r.fieldsOffsetEnd[field._index];

                if (fieldStart === fieldEnd) { continue; }

                if (fieldStart < i || fieldStart > fieldEnd) {
                    throw 'Invalid field positions: ' + JSON.stringify(r);
                }

                if (fieldStart > i) {
                    e.appendChild(document.createTextNode(s.substring(i, fieldStart)));
                }

                field._rendererInstance.append(e, s.substring(fieldStart, fieldEnd), r, rendererCtx);

                i = fieldEnd;
            }

            if (i < s.length) { e.appendChild(document.createTextNode(s.substring(i))); }
        }

        if (r.hasMore) {
            let moreDiv = document.createElement('DIV');
            (<SlElement>moreDiv).virtual = true;
            moreDiv.className = 'has-more';
            moreDiv.innerText =
                '...the log record is too big (' +
                SlUtils.renderFileSize(r.end - r.start) +
                '), only begin of the record is shown ...';
            e.appendChild(moreDiv);
        }

        this.setStrLenAttribute(e);

        let res: HTMLDivElement = <HTMLDivElement>document.createElement('DIV');

        res.className = Record.containPosition(this.vs.selectedLine, r)
            ? 'record selected-line'
            : 'record';

        let logLabel = this.viewConfig.logLabelRenders[r.logId]();
        if (logLabel) {
            res.classList.add('labeled');
            logLabel.style.width = this.logLabelWidth + 'px';
            (<SlElement>logLabel).virtual = true;
            res.appendChild(logLabel);
        }

        res.appendChild(e);

        if (r.filteringError) {
            res.appendChild(
                RecordRendererService.createFilteringErrorIcon(r.filteringError)
            );
        }

        return res;
    }

    renderField(e: HTMLElement, record: Record, field: FieldDescr) {
        let fieldStart = record.fieldsOffsetStart[field._index];
        let fieldEnd = record.fieldsOffsetEnd[field._index];

        if (fieldStart === fieldEnd) {
            return;
        }

        if (fieldStart > fieldEnd) {
            throw 'Invalid field positions: ' + JSON.stringify(record);
        }

        const rendererCtx: RenderContext = {textRenderer: this.textRenderer, compact: false};

        field._rendererInstance.append(e, record.s.substring(fieldStart, fieldEnd), record, rendererCtx);
    }

    initLabelWidthIfNeeded(recordParent: HTMLElement) {
        if (this.logLabelWidth == null) {
            let maxLogLabelWidth = 0;

            let record: HTMLDivElement = null;

            for (let logId of Object.keys(this.viewConfig.logLabelRenders)) {
                let renderer = this.viewConfig.logLabelRenders[logId]();
                if (renderer) {
                    if (!record) {
                        let e: HTMLDivElement = <HTMLDivElement>(
                            document.createElement('DIV')
                        );
                        e.className = 'rec-text';
                        e.textContent = 'foo';

                        record = <HTMLDivElement>document.createElement('DIV');
                        record.className = 'record labeled';

                        record.appendChild(e);
                        recordParent.appendChild(record);
                    }

                    record.insertBefore(renderer, record.firstChild);

                    maxLogLabelWidth = Math.max(maxLogLabelWidth, renderer.clientWidth);
                    record.removeChild(renderer);
                }
            }

            if (record) {
                recordParent.removeChild(record);
            }

            this.logLabelWidth = maxLogLabelWidth;
        }
    }

    clearHighlighting(e: HTMLElement, cls: string) {
        for (let node = e.firstChild; node != null; node = node.nextSibling) {
            if (node.nodeType === Node.ELEMENT_NODE) {
                if ((<SlElement>node).virtual) { continue; }

                if ((<SlElement>node).highlightNode) {
                    (<HTMLElement>node).classList.remove(cls);
                } else {
                    this.clearHighlighting(<HTMLElement>node, cls);
                }
            }
        }
    }

    highlight(e: HTMLElement, start: number, end: number, cls: string) {
        SlUtils.assert(start <= end);
        if (start === end) { return; }

        let nodeStart: number = 0;

        for (let node = e.firstChild; node != null; node = node.nextSibling) {
            if (nodeStart >= end) { break; }

            if (node.nodeType === Node.TEXT_NODE) {
                let val = node.nodeValue;

                if (start < nodeStart + val.length) {
                    if (start > nodeStart) {
                        let headLen = start - nodeStart;
                        e.insertBefore(
                            document.createTextNode(val.substr(0, headLen)),
                            node
                        );

                        nodeStart += headLen;
                        val = val.substr(headLen);
                    }

                    if (nodeStart + val.length <= end) {
                        let span = document.createElement('SPAN');
                        span.innerText = val;
                        span.classList.add(cls);
                        (<SlElement>span).highlightNode = true;
                        (<SlElement>span).strLen = val.length;
                        e.replaceChild(span, node);
                        node = span;
                    } else {
                        let tailLength = val.length - (end - nodeStart);
                        let tail = document.createTextNode(
                            val.substring(val.length - tailLength)
                        );
                        e.replaceChild(tail, node);

                        let span = document.createElement('SPAN');
                        val = val.substr(0, val.length - tailLength);
                        span.innerText = val;
                        (<SlElement>span).highlightNode = true;
                        (<SlElement>span).strLen = val.length;
                        span.classList.add(cls);
                        e.insertBefore(span, tail);
                        return;
                    }
                }

                nodeStart += val.length;
            } else if (node.nodeType === Node.ELEMENT_NODE) {
                if ((<SlElement>node).virtual) { continue; }

                let nodeLen = (<SlElement>node).strLen;

                if (nodeLen === 0) { continue; }

                let nodeE = <HTMLElement>node;

                if (start < nodeStart + nodeLen) {
                    if ((<SlElement>node).highlightNode) {
                        let val = node.firstChild.nodeValue;

                        SlUtils.assert(val.length === nodeLen);

                        if (start > nodeStart) {
                            let headLen = start - nodeStart;
                            let head = document.createElement('SPAN');
                            head.innerText = val.substr(0, headLen);
                            (<SlElement>head).highlightNode = true;
                            (<SlElement>head).strLen = headLen;
                            head.className = nodeE.className;
                            RecordRendererService.copyStyle(head, nodeE);

                            e.insertBefore(head, node);

                            nodeStart += headLen;
                            val = val.substr(headLen);
                            nodeE.innerText = val;
                            nodeLen = val.length;
                            (<SlElement>node).strLen = val.length;
                        }

                        if (nodeStart + val.length <= end) {
                            nodeE.classList.add(cls);
                        } else {
                            let tailLength = val.length - (end - nodeStart);

                            let tailE = document.createElement('SPAN');
                            tailE.innerText = val.substring(val.length - tailLength);
                            (<SlElement>tailE).highlightNode = true;
                            (<SlElement>tailE).strLen = tailLength;
                            tailE.className = nodeE.className;
                            RecordRendererService.copyStyle(tailE, nodeE);

                            e.insertBefore(tailE, node.nextSibling);

                            nodeE.classList.add(cls);
                            val = val.substr(0, val.length - tailLength);
                            nodeE.innerText = val;
                            (<SlElement>nodeE).strLen = val.length;

                            return;
                        }
                    } else {
                        this.highlight(nodeE, start - nodeStart, end - nodeStart, cls);
                    }
                }

                nodeStart += nodeLen;
            }
        }
    }

    expandSelected(r: Record, e: HTMLElement) {
        if (r.searchRes) {
            for (let marker of r.searchRes) {
                this.expand(e, marker.start, marker.end);
            }
        }
    }

    expand(e: HTMLElement, start: number, end: number) {
        SlUtils.assert(start <= end);

        let nodeStart: number = 0;

        for (let node = e.firstChild; node != null; node = node.nextSibling) {
            if (nodeStart > end) { break; }

            if (node.nodeType === Node.TEXT_NODE) {
                let val = node.nodeValue;

                nodeStart += val.length;
            } else if (node.nodeType === Node.ELEMENT_NODE) {
                if ((<SlElement>node).virtual) { continue; }

                let nodeLen = (<SlElement>node).strLen;

                let nodeE = <HTMLElement>node;

                if (start < nodeStart + nodeLen) {
                    if (nodeE.classList.contains('coll-wrapper')) {
                        if (nodeE.classList.contains('collapsed')) {
                            nodeE.classList.remove('collapsed');
                            nodeE.classList.add('expanded');
                        }
                    }

                    this.expand(nodeE, start - nodeStart, end - nodeStart);
                }

                nodeStart += nodeLen;
            }
        }
    }

    setStrLenAttribute(e: HTMLElement): number {
        let existLen = (<SlElement>e).strLen;
        if (existLen != null) { return existLen; }

        let length: number = 0;

        if ((<SlElement>e).virtual) {
            length = 0;
        } else {
            for (let node = e.firstChild; node != null; node = node.nextSibling) {
                if (node.nodeType === Node.TEXT_NODE) {
                    length += node.nodeValue.length;
                } else if (node.nodeType === Node.ELEMENT_NODE) {
                    if ((<SlElement>node).virtual) { continue; }

                    length += this.setStrLenAttribute(<HTMLElement>node);
                }
            }
        }

        (<SlElement>e).strLen = length;

        return length;
    }

    updateSearchResults(record: Record, e: HTMLElement) {
        this.clearHighlighting(e, 'search-result');
        this.addSearchResults(record, e);
    }

    handleClick(event: MouseEvent): boolean {
        for (let e = <Element>event.target; e && !e.classList.contains('rec-text'); e = e.parentElement) {
            if (e.classList.contains('coll-expander')) {
                $($(e).parents('.coll-wrapper')[0])
                    .addClass('expanded')
                    .removeClass('collapsed');
                return true;
            }

            if (e.classList.contains('coll-collapser')) {
                $($(e).parents('.coll-wrapper')[0])
                    .addClass('collapsed')
                    .removeClass('expanded');
                return true;
            }
        }
    }

    private static startWith(s: string, prefix: string): boolean {
        if (!s.startsWith(prefix)) {
            return false;
        }

        return s.length === prefix.length || s[prefix.length] === '/';
    }

    private static copyStyle(target: HTMLElement, source: HTMLElement) {
        target.style.color = source.style.color;
        target.style.fontWeight = source.style.fontWeight;
        target.style.fontStyle = source.style.fontStyle;
    }
}
