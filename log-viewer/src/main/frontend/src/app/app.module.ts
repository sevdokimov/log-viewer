import {NgModule} from '@angular/core';
import {BrowserModule} from '@angular/platform-browser';
import {AppRoot} from './app-root';
import {GlobalNavigation} from './global-nav/global-nav.component';
import {RouterModule, Routes} from '@angular/router';
import {LogNavigatorComponent} from './log-navigator/log-navigator.component';
import {LogViewComponent} from './log-view/log-view.component';
import {FormsModule} from '@angular/forms';
import {FilterPanelComponent} from './log-view/filter-panel.component';
import {SlDurationPipe} from './utils/sl-duration.pipe';
import {SlSizePipe} from './utils/sl-size.pipe';
import {GroovyPredicateEditorComponent} from './log-view/filters/groovy-predicate-editor.component';
import {AceEditorDirective} from './utils/ace-editor.directive';
import {FsTreeItemComponent} from './log-navigator/fs-tree-item.component';
import {FavoritesService} from './services/favorites.service';
import {LogListPanelComponent} from './log-view/log-list-panel.component';
import {BsDropdownModule, ModalModule} from 'ngx-bootstrap';
import {ToastrModule} from 'ngx-toastr';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {HttpClientModule} from '@angular/common/http';
import {TopFilterListComponent} from './log-view/top-filters/top-filter-list/top-filter-list.component';
import {LevelListComponent} from './log-view/top-filters/level-list/level-list.component';
import {ExceptionOnlyComponent} from '@app/log-view/top-filters/exception-only/exception-only.component';
import {EventDetailsComponent} from '@app/log-view/event-details/event-details.component';
import {FileStatusComponent} from '@app/log-view/file-status/file-status.component';
import {ContextMenuModule} from 'ngx-contextmenu';

export const appRoutes: Routes = [
    {
        path: '',
        component: LogNavigatorComponent,
        data: {title: 'Log navigator'},
    },
    {
        path: 'sources',
        component: LogNavigatorComponent,
        data: {title: 'Log navigator'},
    },
    {path: 'log', component: LogViewComponent, data: {title: 'Log view'}},
];

@NgModule({
    imports: [
        BrowserModule,
        BrowserAnimationsModule,
        HttpClientModule,
        BsDropdownModule.forRoot(),
        ModalModule.forRoot(),
        FormsModule,
        ToastrModule.forRoot(),
        ContextMenuModule.forRoot({
            useBootstrap4: true,
        }),
        //        ReactiveFormsModule,

        // TranslateModule.forRoot(),
        //        Ng2BootstrapModule.forRoot(),

        RouterModule.forRoot(appRoutes),
    ],
    declarations: [
        // root
        AppRoot,

        GlobalNavigation,
        // SimpleNotificationsComponent,
        LogNavigatorComponent,
        FsTreeItemComponent,
        LogViewComponent,
        FilterPanelComponent,
        LogListPanelComponent,
        GroovyPredicateEditorComponent,
        EventDetailsComponent,
        FileStatusComponent,

        AceEditorDirective,
        SlDurationPipe,
        SlSizePipe,
        TopFilterListComponent,
        LevelListComponent,
        ExceptionOnlyComponent,
    ],
    providers: [FavoritesService],

    exports: [SlDurationPipe, SlSizePipe],

    bootstrap: [AppRoot],
})
export class AppModule {
}
