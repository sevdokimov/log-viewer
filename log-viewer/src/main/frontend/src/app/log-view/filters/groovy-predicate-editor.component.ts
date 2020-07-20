import {Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges, ViewChild} from '@angular/core';
import {GroovyPredicate} from '../predicates';
import {AceEditorDirective} from '@app/utils/ace-editor.directive';
import {Filter} from '@app/log-view/filter-panel-state.service';

@Component({
    selector: 'sl-groovy-predicate-editor',
    templateUrl: './groovy-predicate-editor.template.html',
    styleUrls: ['./groovy-predicate-editor.style.scss']
})
export class GroovyPredicateEditorComponent implements OnChanges, OnInit {

    @Input() filter: Filter;

    @Input() forceEditing: boolean;

    @Output() saved = new EventEmitter<boolean>();

    scriptEditorOptions = {
        maxLines: 10,
        minLines: 1,
        showPrintMargin: false,
        highlightActiveLine: false,
        highlightGutterLine: false,
        setShowGutter: false,
        showLineNumbers: false,
        showGutter: false,
        fixedWidthGutter: false,
        fontSize: 14,
    };

    editing: boolean;

    scriptBackup: string;

    @ViewChild(AceEditorDirective, {static: false})
    private aceEditor: AceEditorDirective;

    ngOnInit(): void {
        if (this.forceEditing) {
            this.startEditing();
        }
    }

    startEditing() {
        this.editing = true;
        this.scriptBackup = (<GroovyPredicate>this.filter.predicate).script;
    }

    stopEditing() {
        this.editing = false;
        this.saved.emit(true);
    }

    revert() {
        if (!this.editing) { return; }

        this.editing = false;
        (<GroovyPredicate>this.filter.predicate).script = this.scriptBackup;

        this.saved.emit(true);
    }

    onTextChanged(newText: string) {
        (<GroovyPredicate>this.filter.predicate).script = newText;
    }

    editorKeyDown(event: KeyboardEvent) {
        switch (event.keyCode) {
            case 13:
                if (event.ctrlKey) {
                    this.stopEditing();
                    break;
                }
                return true;

            case 90: // z
                if (event.ctrlKey && event.altKey) {
                    this.revert();
                    break;
                }
                return true;

            default:
                return true;
        }

        event.preventDefault();
        return false;
    }

    ngOnChanges(changes: SimpleChanges) {
    }
}
