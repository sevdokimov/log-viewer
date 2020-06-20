import {AfterViewInit, Directive, ElementRef, EventEmitter, Input, OnDestroy, Output} from '@angular/core';
import * as ace from 'brace';
import 'brace/theme/monokai';
import 'brace/mode/groovy';

@Directive({
    selector: '[behAceEditor]'
})
export class AceEditorDirective implements AfterViewInit, OnDestroy {
    @Output('textChanged') textChanged = new EventEmitter();
    _options: any = {};
    _readOnly: boolean = false;
    _theme: string = 'monokai';
    _mode: string = 'groovy';
    _autoUpdateContent: boolean = true;
    editor: any;
    oldText: any;

    constructor(elementRef: ElementRef) {
        let el = elementRef.nativeElement;
        this.editor = ace['edit'](el);

        this.init(elementRef);
        this.initEvents();
    }

    init(elementRef: ElementRef) {
        this.editor.setOptions(this._options || {});
        // this.editor.setTheme(`ace/theme/${this._theme}`);
        this.editor.getSession().setMode(`ace/mode/${this._mode}`);
        this.editor.setReadOnly(this._readOnly);
        this.editor.setAutoScrollEditorIntoView(true);

        let id = elementRef.nativeElement.id;
        elementRef.nativeElement.id = '';
        this.editor.textInput.getElement().id = id;
    }

    initEvents() {
        this.editor.on('change', () => {
            let newVal = this.editor.getValue();
            if (newVal === this.oldText) { return; }
            if (typeof this.oldText !== 'undefined') { this.textChanged.emit(newVal); }
            this.oldText = newVal;
        });
    }

    @Input()
    set options(options: any) {
        this._options = options;
        this.editor.setOptions(options || {});
    }

    @Input()
    set readOnly(readOnly: any) {
        this._readOnly = readOnly;
        this.editor.setReadOnly(readOnly);
    }

    @Input()
    set theme(theme: any) {
        this._theme = theme;
        this.editor.setTheme(`ace/theme/${theme}`);
    }

    @Input()
    set mode(mode: any) {
        this._mode = mode;
        this.editor.getSession().setMode(`ace/mode/${mode}`);
    }

    @Input()
    set disableFocus(f: boolean) {
        this.editor.textInput.getElement().disabled = f;
    }

    @Input()
    set text(text: any) {
        if (text == null) { text = ''; }

        if (this._autoUpdateContent === true) {
            if (text === this.oldText) return;

            this.editor.setValue(text);
            this.editor.clearSelection();
            this.editor.focus();

            this.oldText = text;
        } else {
            if (this.oldText === undefined) { this.oldText = text; }
        }
    }

    @Input()
    set autoUpdateContent(status: any) {
        this._autoUpdateContent = status;
    }

    ngAfterViewInit() {
        this.editor.resize();
        this.editor.gotoLine(1, 0);
    }

    ngOnDestroy() {
        if (this.editor) {
            this.editor.destroy();
        }
    }
}
