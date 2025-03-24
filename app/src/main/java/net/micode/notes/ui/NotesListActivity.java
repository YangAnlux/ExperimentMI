/*
 * Copyright (c) 2010-2011, The MiCode Open Source Community (www.micode.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.micode.notes.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.appwidget.AppWidgetManager;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Display;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.View.OnTouchListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import net.micode.notes.R;
import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.NoteColumns;
import net.micode.notes.gtask.remote.GTaskSyncService;
import net.micode.notes.model.WorkingNote;
import net.micode.notes.tool.BackupUtils;
import net.micode.notes.tool.DataUtils;
import net.micode.notes.tool.ResourceParser;
import net.micode.notes.ui.NotesListAdapter.AppWidgetAttribute;
import net.micode.notes.widget.NoteWidgetProvider_2x;
import net.micode.notes.widget.NoteWidgetProvider_4x;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;

// 笔记列表活动类，用于展示和管理笔记列表
public class NotesListActivity extends Activity implements OnClickListener, OnItemLongClickListener {

    // 文件夹笔记列表查询的令牌
    private static final int FOLDER_NOTE_LIST_QUERY_TOKEN = 0;
    // 文件夹列表查询的令牌
    private static final int FOLDER_LIST_QUERY_TOKEN = 1;
    // 文件夹删除菜单项的ID
    private static final int MENU_FOLDER_DELETE = 0;
    // 文件夹查看菜单项的ID
    private static final int MENU_FOLDER_VIEW = 1;
    // 文件夹重命名菜单项的ID
    private static final int MENU_FOLDER_CHANGE_NAME = 2;
    // 存储应用介绍是否已添加的偏好设置键
    private static final String PREFERENCE_ADD_INTRODUCTION = "net.micode.notes.introduction";

    // 定义列表编辑状态的枚举
    private enum ListEditState {
        NOTE_LIST,  // 笔记列表状态
        SUB_FOLDER, // 子文件夹状态
        CALL_RECORD_FOLDER // 通话记录文件夹状态
    }

    // 当前的列表编辑状态
    private ListEditState mState;
    // 后台查询处理器，用于异步查询数据库
    private BackgroundQueryHandler mBackgroundQueryHandler;
    // 笔记列表适配器，用于填充笔记列表数据
    private NotesListAdapter mNotesListAdapter;
    // 笔记列表视图
    private ListView mNotesListView;
    // 添加新笔记的按钮
    private Button mAddNewNote;
    // 用于标记触摸事件是否需要分发
    private boolean mDispatch;
    // 触摸事件起始的Y坐标
    private int mOriginY;
    // 触摸事件分发的Y坐标
    private int mDispatchY;
    // 标题栏文本视图
    private TextView mTitleBar;
    // 当前文件夹的ID
    private long mCurrentFolderId;
    // 内容解析器，用于操作内容提供者
    private ContentResolver mContentResolver;
    // 用于处理列表视图多选模式的回调类
    private ModeCallback mModeCallBack;
    // 日志标签
    private static final String TAG = "NotesListActivity";
    // 笔记列表视图滚动速率
    public static final int NOTES_LISTVIEW_SCROLL_RATE = 30;
    // 当前聚焦的笔记项数据
    private NoteItemData mFocusNoteDataItem;
    // 普通选择条件，用于查询指定父文件夹下的笔记
    private static final String NORMAL_SELECTION = NoteColumns.PARENT_ID + "=?";
    // 根文件夹选择条件，用于查询根文件夹下的笔记和通话记录文件夹（如果有记录）
    private static final String ROOT_FOLDER_SELECTION = "(" + NoteColumns.TYPE + "<>"
            + Notes.TYPE_SYSTEM + " AND " + NoteColumns.PARENT_ID + "=?)" + " OR ("
            + NoteColumns.ID + "=" + Notes.ID_CALL_RECORD_FOLDER + " AND "
            + NoteColumns.NOTES_COUNT + ">0)";
    // 打开节点（笔记或文件夹）的请求码
    private final static int REQUEST_CODE_OPEN_NODE = 102;
    // 新建节点（笔记或文件夹）的请求码
    private final static int REQUEST_CODE_NEW_NODE = 103;
    // 更改背景的按钮
    private Button btn_change_background;
    // 笔记列表的帧布局
    private FrameLayout fl_note_list;
    // 笔记列表背景资源数组
    private final int[] fl_note_list_backgrounds = {
            R.drawable.pengyuyan, R.drawable.huojianhua, R.drawable.xuezhiqian,
            R.drawable.wangsulong, R.drawable.huge
    };
    // 当前背景资源的索引
    private int curIndex = 0;

    // 活动创建时调用的方法
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.note_list);
        initResources();

        // 首次使用应用时插入介绍笔记
        setAppInfoFromRawRes();
    }

    // 处理活动结果的方法
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK
                && (requestCode == REQUEST_CODE_OPEN_NODE || requestCode == REQUEST_CODE_NEW_NODE)) {
            mNotesListAdapter.changeCursor(null);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    // 从原始资源文件中读取应用介绍并插入为笔记的方法
    private void setAppInfoFromRawRes() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        if (!sp.getBoolean(PREFERENCE_ADD_INTRODUCTION, false)) {
            StringBuilder sb = new StringBuilder();
            InputStream in = null;
            try {
                in = getResources().openRawResource(R.raw.introduction);
                if (in != null) {
                    InputStreamReader isr = new InputStreamReader(in);
                    BufferedReader br = new BufferedReader(isr);
                    char[] buf = new char[1024];
                    int len = 0;
                    while ((len = br.read(buf)) > 0) {
                        sb.append(buf, 0, len);
                    }
                } else {
                    Log.e(TAG, "Read introduction file error");
                    return;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return;
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            WorkingNote note = WorkingNote.createEmptyNote(this, Notes.ID_ROOT_FOLDER,
                    AppWidgetManager.INVALID_APPWIDGET_ID, Notes.TYPE_WIDGET_INVALIDE,
                    ResourceParser.RED);
            note.setWorkingText(sb.toString());
            if (note.saveNote()) {
                sp.edit().putBoolean(PREFERENCE_ADD_INTRODUCTION, true).commit();
            } else {
                Log.e(TAG, "Save introduction note error");
                return;
            }
        }
    }

    // 活动开始时调用的方法
    @Override
    protected void onStart() {
        super.onStart();
        startAsyncNotesListQuery();
    }

    // 初始化资源的方法
    private void initResources() {
        mContentResolver = this.getContentResolver();
        mBackgroundQueryHandler = new BackgroundQueryHandler(this.getContentResolver());
        mCurrentFolderId = Notes.ID_ROOT_FOLDER;
        mNotesListView = (ListView) findViewById(R.id.notes_list);
        mNotesListView.addFooterView(LayoutInflater.from(this).inflate(R.layout.note_list_footer, null),
                null, false);
        mNotesListView.setOnItemClickListener(new OnListItemClickListener());
        mNotesListView.setOnItemLongClickListener(this);
        mNotesListAdapter = new NotesListAdapter(this);
        mNotesListView.setAdapter(mNotesListAdapter);
        mAddNewNote = (Button) findViewById(R.id.btn_new_note);
        mAddNewNote.setOnClickListener(this);
        mAddNewNote.setOnTouchListener(new NewNoteOnTouchListener());
        btn_change_background = findViewById(R.id.btn_change_background);
        fl_note_list = findViewById(R.id.fl_note_list);
        btn_change_background.setOnClickListener(this);
        mDispatch = false;
        mDispatchY = 0;
        mOriginY = 0;
        mTitleBar = (TextView) findViewById(R.id.tv_title_bar);
        mState = ListEditState.NOTE_LIST;
        mModeCallBack = new ModeCallback();
    }

    // 处理列表视图多选模式和菜单项点击的回调类
    private class ModeCallback implements ListView.MultiChoiceModeListener, OnMenuItemClickListener {
        private DropdownMenu mDropDownMenu;
        private ActionMode mActionMode;
        private MenuItem mMoveMenu;

        // 创建多选模式的操作栏时调用的方法
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            getMenuInflater().inflate(R.menu.note_list_options, menu);
            menu.findItem(R.id.delete).setOnMenuItemClickListener(this);
            mMoveMenu = menu.findItem(R.id.move);
            // 根据笔记项的父文件夹ID和用户文件夹数量设置移动菜单项的可见性
            if (mFocusNoteDataItem.getParentId() == Notes.ID_CALL_RECORD_FOLDER
                    || DataUtils.getUserFolderCount(mContentResolver) == 0) {
                mMoveMenu.setVisible(false);
            } else {
                mMoveMenu.setVisible(true);
                mMoveMenu.setOnMenuItemClickListener(this);
            }
            mActionMode = mode;
            mNotesListAdapter.setChoiceMode(true);
            mNotesListView.setLongClickable(false);
            mAddNewNote.setVisibility(View.GONE);

            View customView = LayoutInflater.from(NotesListActivity.this).inflate(
                    R.layout.note_list_dropdown_menu, null);
            mode.setCustomView(customView);
            mDropDownMenu = new DropdownMenu(NotesListActivity.this,
                    (Button) customView.findViewById(R.id.selection_menu),
                    R.menu.note_list_dropdown);
            mDropDownMenu.setOnDropdownMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                public boolean onMenuItemClick(MenuItem item) {
                    mNotesListAdapter.selectAll(!mNotesListAdapter.isAllSelected());
                    updateMenu();
                    return true;
                }
            });
            return true;
        }

        // 更新下拉菜单的方法
        private void updateMenu() {
            int selectedCount = mNotesListAdapter.getSelectedCount();
            // 更新下拉菜单的标题
            String format = getResources().getString(R.string.menu_select_title, selectedCount);
            mDropDownMenu.setTitle(format);
            MenuItem item = mDropDownMenu.findItem(R.id.action_select_all);
            if (item != null) {
                if (mNotesListAdapter.isAllSelected()) {
                    item.setChecked(true);
                    item.setTitle(R.string.menu_deselect_all);
                } else {
                    item.setChecked(false);
                    item.setTitle(R.string.menu_select_all);
                }
            }
        }

        // 准备多选模式的操作栏时调用的方法（未实现）
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            // TODO Auto-generated method stub
            return false;
        }

        // 点击操作栏中的菜单项时调用的方法（未实现）
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            // TODO Auto-generated method stub
            return false;
        }

        // 销毁多选模式的操作栏时调用的方法
        public void onDestroyActionMode(ActionMode mode) {
            mNotesListAdapter.setChoiceMode(false);
            mNotesListView.setLongClickable(true);
            mAddNewNote.setVisibility(View.VISIBLE);
        }

        // 结束多选模式的操作栏的方法
        public void finishActionMode() {
            mActionMode.finish();
        }

        // 当列表项的选中状态改变时调用的方法
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id,
                                              boolean checked) {
            mNotesListAdapter.setCheckedItem(position, checked);
            updateMenu();
        }

        // 点击菜单项时调用的方法
        public boolean onMenuItemClick(MenuItem item) {
            if (mNotesListAdapter.getSelectedCount() == 0) {
                Toast.makeText(NotesListActivity.this, getString(R.string.menu_select_none),
                        Toast.LENGTH_SHORT).show();
                return true;
            }

            switch (item.getItemId()) {
                case R.id.delete:
                    AlertDialog.Builder builder = new AlertDialog.Builder(NotesListActivity.this);
                    builder.setTitle(getString(R.string.alert_title_delete));
                    builder.setIcon(android.R.drawable.ic_dialog_alert);
                    builder.setMessage(getString(R.string.alert_message_delete_notes,
                            mNotesListAdapter.getSelectedCount()));
                    builder.setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                                    int which) {
                                    batchDelete();
                                }
                            });
                    builder.setNegativeButton(android.R.string.cancel, null);
                    builder.show();
                    break;
                case R.id.move:
                    startQueryDestinationFolders();
                    break;
                default:
                    return false;
            }
            return true;
        }
    }

    // 处理添加新笔记按钮触摸事件的监听器类
    private class NewNoteOnTouchListener implements OnTouchListener {

        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: {
                    Display display = getWindowManager().getDefaultDisplay();
                    int screenHeight = display.getHeight();
                    int newNoteViewHeight = mAddNewNote.getHeight();
                    int start = screenHeight - newNoteViewHeight;
                    int eventY = start + (int) event.getY();
                    // 如果是子文件夹状态，减去标题栏的高度
                    if (mState == ListEditState.SUB_FOLDER) {
                        eventY -= mTitleBar.getHeight();
                        start -= mTitleBar.getHeight();
                    }
                    // 处理点击按钮透明部分的逻辑，将事件分发给后面的列表视图
                    if (event.getY() < (event.getX() * (-0.12) + 94)) {
                        View view = mNotesListView.getChildAt(mNotesListView.getChildCount() - 1
                                - mNotesListView.getFooterViewsCount());
                        if (view != null && view.getBottom() > start
                                && (view.getTop() < (start + 94))) {
                            mOriginY = (int) event.getY();
                            mDispatchY = eventY;
                            event.setLocation(event.getX(), mDispatchY);
                            mDispatch = true;
                            return mNotesListView.dispatchTouchEvent(event);
                        }
                    }
                    break;
                }
                case MotionEvent.ACTION_MOVE: {
                    if (mDispatch) {
                        mDispatchY += (int) event.getY() - mOriginY;
                        event.setLocation(event.getX(), mDispatchY);
                        return mNotesListView.dispatchTouchEvent(event);
                    }
                    break;
                }
                default: {
                    if (mDispatch) {
                        event.setLocation(event.getX(), mDispatchY);
                        mDispatch = false;
                        return mNotesListView.dispatchTouchEvent(event);
                    }
                    break;
                }
            }
            return false;
        }

    }

// 启动异步查询笔记列表数据的方法
private void startAsyncNotesListQuery() {
    // 根据当前文件夹ID选择查询条件
    String selection = (mCurrentFolderId == Notes.ID_ROOT_FOLDER)? ROOT_FOLDER_SELECTION
            : NORMAL_SELECTION;
    // 启动异步查询
    mBackgroundQueryHandler.startQuery(FOLDER_NOTE_LIST_QUERY_TOKEN, null,
            Notes.CONTENT_NOTE_URI, NoteItemData.PROJECTION, selection, new String[]{
                    String.valueOf(mCurrentFolderId)
            }, NoteColumns.TYPE + " DESC," + NoteColumns.MODIFIED_DATE + " DESC");
}

// 异步查询处理器内部类
private final class BackgroundQueryHandler extends AsyncQueryHandler {
    public BackgroundQueryHandler(ContentResolver contentResolver) {
        super(contentResolver);
    }

    // 查询完成时的回调方法
    @Override
    protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
        switch (token) {
            case FOLDER_NOTE_LIST_QUERY_TOKEN:
                // 更新笔记列表适配器的游标
                mNotesListAdapter.changeCursor(cursor);
                break;
            case FOLDER_LIST_QUERY_TOKEN:
                if (cursor != null && cursor.getCount() > 0) {
                    // 显示文件夹列表菜单
                    showFolderListMenu(cursor);
                } else {
                    Log.e(TAG, "Query folder failed");
                }
                break;
            default:
                return;
        }
    }
}

// 显示文件夹列表菜单的方法
private void showFolderListMenu(Cursor cursor) {
    AlertDialog.Builder builder = new AlertDialog.Builder(NotesListActivity.this);
    builder.setTitle(R.string.menu_title_select_folder);
    // 创建文件夹列表适配器
    final FoldersListAdapter adapter = new FoldersListAdapter(this, cursor);
    builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            // 将选中的笔记移动到指定文件夹
            DataUtils.batchMoveToFolder(mContentResolver,
                    mNotesListAdapter.getSelectedItemIds(), adapter.getItemId(which));
            Toast.makeText(
                    NotesListActivity.this,
                    getString(R.string.format_move_notes_to_folder,
                            mNotesListAdapter.getSelectedCount(),
                            adapter.getFolderName(NotesListActivity.this, which)),
                    Toast.LENGTH_SHORT).show();
            // 结束多选模式
            mModeCallBack.finishActionMode();
        }
    });
    builder.show();
}

// 创建新笔记的方法
private void createNewNote() {
    Intent intent = new Intent(this, NoteEditActivity.class);
    intent.setAction(Intent.ACTION_INSERT_OR_EDIT);
    intent.putExtra(Notes.INTENT_EXTRA_FOLDER_ID, mCurrentFolderId);
    // 启动笔记编辑活动并等待结果
    this.startActivityForResult(intent, REQUEST_CODE_NEW_NODE);
}

// 批量删除选中笔记的方法
private void batchDelete() {
    new AsyncTask<Void, Void, HashSet<AppWidgetAttribute>>() {
        protected HashSet<AppWidgetAttribute> doInBackground(Void... unused) {
            // 获取选中笔记对应的小部件信息
            HashSet<AppWidgetAttribute> widgets = mNotesListAdapter.getSelectedWidget();
            if (!isSyncMode()) {
                // 非同步模式下直接删除笔记
                if (DataUtils.batchDeleteNotes(mContentResolver, mNotesListAdapter
                        .getSelectedItemIds())) {
                } else {
                    Log.e(TAG, "Delete notes error, should not happens");
                }
            } else {
                // 同步模式下将笔记移动到回收站文件夹
                if (!DataUtils.batchMoveToFolder(mContentResolver, mNotesListAdapter
                        .getSelectedItemIds(), Notes.ID_TRASH_FOLER)) {
                    Log.e(TAG, "Move notes to trash folder error, should not happens");
                }
            }
            return widgets;
        }

        @Override
        protected void onPostExecute(HashSet<AppWidgetAttribute> widgets) {
            if (widgets != null) {
                for (AppWidgetAttribute widget : widgets) {
                    if (widget.widgetId != AppWidgetManager.INVALID_APPWIDGET_ID
                            && widget.widgetType != Notes.TYPE_WIDGET_INVALIDE) {
                        // 更新相关小部件
                        updateWidget(widget.widgetId, widget.widgetType);
                    }
                }
            }
            // 结束多选模式
            mModeCallBack.finishActionMode();
        }
    }.execute();
}

// 删除文件夹的方法
private void deleteFolder(long folderId) {
    if (folderId == Notes.ID_ROOT_FOLDER) {
        Log.e(TAG, "Wrong folder id, should not happen " + folderId);
        return;
    }

    HashSet<Long> ids = new HashSet<Long>();
    ids.add(folderId);
    // 获取文件夹下笔记对应的小部件信息
    HashSet<AppWidgetAttribute> widgets = DataUtils.getFolderNoteWidget(mContentResolver,
            folderId);
    if (!isSyncMode()) {
        // 非同步模式下直接删除文件夹及其中的笔记
        DataUtils.batchDeleteNotes(mContentResolver, ids);
    } else {
        // 同步模式下将文件夹及其中的笔记移动到回收站文件夹
        DataUtils.batchMoveToFolder(mContentResolver, ids, Notes.ID_TRASH_FOLER);
    }
    if (widgets != null) {
        for (AppWidgetAttribute widget : widgets) {
            if (widget.widgetId != AppWidgetManager.INVALID_APPWIDGET_ID
                    && widget.widgetType != Notes.TYPE_WIDGET_INVALIDE) {
                // 更新相关小部件
                updateWidget(widget.widgetId, widget.widgetType);
            }
        }
    }
}

// 打开笔记的方法
private void openNode(NoteItemData data) {
    Intent intent = new Intent(this, NoteEditActivity.class);
    intent.setAction(Intent.ACTION_VIEW);
    intent.putExtra(Intent.EXTRA_UID, data.getId());
    // 启动笔记编辑活动并等待结果
    this.startActivityForResult(intent, REQUEST_CODE_OPEN_NODE);
}

// 打开文件夹的方法
private void openFolder(NoteItemData data) {
    mCurrentFolderId = data.getId();
    startAsyncNotesListQuery();
    if (data.getId() == Notes.ID_CALL_RECORD_FOLDER) {
        mState = ListEditState.CALL_RECORD_FOLDER;
        mAddNewNote.setVisibility(View.GONE);
    } else {
        mState = ListEditState.SUB_FOLDER;
    }
    if (data.getId() == Notes.ID_CALL_RECORD_FOLDER) {
        mTitleBar.setText(R.string.call_record_folder_name);
    } else {
        mTitleBar.setText(data.getSnippet());
    }
    mTitleBar.setVisibility(View.VISIBLE);
}

// 处理视图点击事件的方法
public void onClick(View v) {
    switch (v.getId()) {
        case R.id.btn_new_note:
            createNewNote();
            break;
        case R.id.btn_change_background:
            // 切换笔记列表背景
            fl_note_list.setBackgroundResource(fl_note_list_backgrounds[curIndex]);
            curIndex = (curIndex + 1) % fl_note_list_backgrounds.length;
            break;
        default:
            break;
    }
}

// 显示软键盘的方法
private void showSoftInput() {
    InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
    if (inputMethodManager != null) {
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }
}

// 隐藏软键盘的方法
private void hideSoftInput(View view) {
    InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
    inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
}

// 显示创建或修改文件夹对话框的方法
private void showCreateOrModifyFolderDialog(final boolean create) {
    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
    View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_text, null);
    final EditText etName = (EditText) view.findViewById(R.id.et_foler_name);
    showSoftInput();
    if (!create) {
        if (mFocusNoteDataItem != null) {
            etName.setText(mFocusNoteDataItem.getSnippet());
            builder.setTitle(getString(R.string.menu_folder_change_name));
        } else {
            Log.e(TAG, "The long click data item is null");
            return;
        }
    } else {
        etName.setText("");
        builder.setTitle(this.getString(R.string.menu_create_folder));
    }

    builder.setPositiveButton(android.R.string.ok, null);
    builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            hideSoftInput(etName);
        }
    });

    final Dialog dialog = builder.setView(view).show();
    final Button positive = (Button) dialog.findViewById(android.R.id.button1);
    positive.setOnClickListener(new OnClickListener() {
        public void onClick(View v) {
            hideSoftInput(etName);
            String name = etName.getText().toString();
            if (DataUtils.checkVisibleFolderName(mContentResolver, name)) {
                Toast.makeText(NotesListActivity.this, getString(R.string.folder_exist, name),
                        Toast.LENGTH_LONG).show();
                etName.setSelection(0, etName.length());
                return;
            }
            if (!create) {
                if (!TextUtils.isEmpty(name)) {
                    ContentValues values = new ContentValues();
                    values.put(NoteColumns.SNIPPET, name);
                    values.put(NoteColumns.TYPE, Notes.TYPE_FOLDER);
                    values.put(NoteColumns.LOCAL_MODIFIED, 1);
                    mContentResolver.update(Notes.CONTENT_NOTE_URI, values, NoteColumns.ID
                            + "=?", new String[]{
                            String.valueOf(mFocusNoteDataItem.getId())
                    });
                }
            } else if (!TextUtils.isEmpty(name)) {
                ContentValues values = new ContentValues();
                values.put(NoteColumns.SNIPPET, name);
                values.put(NoteColumns.TYPE, Notes.TYPE_FOLDER);
                mContentResolver.insert(Notes.CONTENT_NOTE_URI, values);
            }
            dialog.dismiss();
        }
    });

    if (TextUtils.isEmpty(etName.getText())) {
        positive.setEnabled(false);
    }
    // 监听输入框文本变化，控制确定按钮的启用状态
    etName.addTextChangedListener(new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // 未实现
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (TextUtils.isEmpty(etName.getText())) {
                positive.setEnabled(false);
            } else {
                positive.setEnabled(true);
            }
        }

        public void afterTextChanged(Editable s) {
            // 未实现
        }
    });
}

// 处理返回键点击事件的方法
@Override
public void onBackPressed() {
    switch (mState) {
        case SUB_FOLDER:
            mCurrentFolderId = Notes.ID_ROOT_FOLDER;
            mState = ListEditState.NOTE_LIST;
            startAsyncNotesListQuery();
            mTitleBar.setVisibility(View.GONE);
            break;
        case CALL_RECORD_FOLDER:
            mCurrentFolderId = Notes.ID_ROOT_FOLDER;
            mState = ListEditState.NOTE_LIST;
            mAddNewNote.setVisibility(View.VISIBLE);
            mTitleBar.setVisibility(View.GONE);
            startAsyncNotesListQuery();
            break;
        case NOTE_LIST:
            super.onBackPressed();
            break;
        default:
            break;
    }
}

// 更新小部件的方法
private void updateWidget(int appWidgetId, int appWidgetType) {
    Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
    if (appWidgetType == Notes.TYPE_WIDGET_2X) {
        intent.setClass(this, NoteWidgetProvider_2x.class);
    } else if (appWidgetType == Notes.TYPE_WIDGET_4X) {
        intent.setClass(this, NoteWidgetProvider_4x.class);
    } else {
        Log.e(TAG, "Unspported widget type");
        return;
    }

    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{
            appWidgetId
    });

    sendBroadcast(intent);
    setResult(RESULT_OK, intent);
}

// 文件夹创建上下文菜单的监听器
private final OnCreateContextMenuListener mFolderOnCreateContextMenuListener = new OnCreateContextMenuListener() {
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        if (mFocusNoteDataItem != null) {
            menu.setHeaderTitle(mFocusNoteDataItem.getSnippet());
            menu.add(0, MENU_FOLDER_VIEW, 0, R.string.menu_folder_view);
            menu.add(0, MENU_FOLDER_DELETE, 0, R.string.menu_folder_delete);
            menu.add(0, MENU_FOLDER_CHANGE_NAME, 0, R.string.menu_folder_change_name);
        }
    }
};

// 上下文菜单关闭时的回调方法
@Override
public void onContextMenuClosed(Menu menu) {
    if (mNotesListView != null) {
        mNotesListView.setOnCreateContextMenuListener(null);
    }
    super.onContextMenuClosed(menu);
}

// 处理上下文菜单项选择事件的方法
@Override
public boolean onContextItemSelected(MenuItem item) {
    if (mFocusNoteDataItem == null) {
        Log.e(TAG, "The long click data item is null");
        return false;
    }
    switch (item.getItemId()) {
        case MENU_FOLDER_VIEW:
            openFolder(mFocusNoteDataItem);
            break;
        case MENU_FOLDER_DELETE:
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.alert_title_delete));
            builder.setIcon(android.R.drawable.ic_dialog_alert);
            builder.setMessage(getString(R.string.alert_message_delete_folder));
            builder.setPositiveButton(android.R.string.ok,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            deleteFolder(mFocusNoteDataItem.getId());
                        }
                    });
            builder.setNegativeButton(android.R.string.cancel, null);
            builder.show();
            break;
        case MENU_FOLDER_CHANGE_NAME:
            showCreateOrModifyFolderDialog(false);
            break;
        default:
            break;
    }

    return true;
}

// 准备选项菜单时的回调方法
@Override
public boolean onPrepareOptionsMenu(Menu menu) {
    menu.clear();
    if (mState == ListEditState.NOTE_LIST) {
        getMenuInflater().inflate(R.menu.note_list, menu);
        // 根据同步状态设置同步菜单项的标题
        menu.findItem(R.id.menu_sync).setTitle(
                GTaskSyncService.isSyncing()? R.string.menu_sync_cancel : R.string.menu_sync);
    } else if (mState == ListEditState.SUB_FOLDER) {
        getMenuInflater().inflate(R.menu.sub_folder, menu);
    } else if (mState == ListEditState.CALL_RECORD_FOLDER) {
        getMenuInflater().inflate(R.menu.call_record_folder, menu);
    } else {
        Log.e(TAG, "Wrong state:" + mState);
    }
    return true;
}

// 处理选项菜单项选择事件的方法
@Override
public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
        case R.id.menu_new_folder: {
            showCreateOrModifyFolderDialog(true);
            break;
        }
        case R.id.menu_export_text: {
            exportNoteToText();
            break;
        }
        case R.id.menu_sync: {
            if (isSyncMode()) {
                if (TextUtils.equals(item.getTitle(), getString(R.string.menu_sync))) {
                    GTaskSyncService.startSync(this);
                } else {
                    GTaskSyncService.cancelSync(this);
                }
            } else {
                startPreferenceActivity();
            }
            break;
        }
        case R.id.menu_setting: {
            startPreferenceActivity();
            break;
        }
        case R.id.menu_new_note: {
            createNewNote();
            break;
        }
        case R.id.menu_search:
            onSearchRequested();
            break;
        default:
            break;
    }
    return true;
}

// 处理搜索请求的方法
@Override
public boolean onSearchRequested() {
    startSearch(null, false, null /* appData */, false);
    return true;
}

// 将笔记导出为文本文件的方法
private void exportNoteToText() {
    final BackupUtils backup = BackupUtils.getInstance(NotesListActivity.this);
    new AsyncTask<Void, Void, Integer>() {
        @Override
        protected Integer doInBackground(Void... unused) {
            return backup.exportToText();
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (result == BackupUtils.STATE_SD_CARD_UNMOUONTED) {
                AlertDialog.Builder builder = new AlertDialog.Builder(NotesListActivity.this);
                builder.setTitle(NotesListActivity.this
                        .getString(R.string.failed_sdcard_export));
                                        builder.setMessage(NotesListActivity.this
                                .getString(R.string.error_sdcard_unmounted));
                        builder.setPositiveButton(android.R.string.ok, null);
                        builder.show();
                    } else if (result == BackupUtils.STATE_SUCCESS) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(NotesListActivity.this);
                        builder.setTitle(NotesListActivity.this
                                .getString(R.string.success_sdcard_export));
                        builder.setMessage(NotesListActivity.this.getString(
                                R.string.format_exported_file_location, backup
                                        .getExportedTextFileName(), backup.getExportedTextFileDir()));
                        builder.setPositiveButton(android.R.string.ok, null);
                        builder.show();
                    } else if (result == BackupUtils.STATE_SYSTEM_ERROR) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(NotesListActivity.this);
                        builder.setTitle(NotesListActivity.this
                                .getString(R.string.failed_sdcard_export));
                        builder.setMessage(NotesListActivity.this
                                .getString(R.string.error_sdcard_export));
                        builder.setPositiveButton(android.R.string.ok, null);
                        builder.show();
                    }
                }

            }.execute();
        }

        // 判断是否处于同步模式的方法
        private boolean isSyncMode() {
            return NotesPreferenceActivity.getSyncAccountName(this).trim().length() > 0;
        }

        // 启动偏好设置活动的方法
        private void startPreferenceActivity() {
            Activity from = getParent() != null ? getParent() : this;
            Intent intent = new Intent(from, NotesPreferenceActivity.class);
            from.startActivityIfNeeded(intent, -1);
        }

        // 列表项点击监听器类
        private class OnListItemClickListener implements OnItemClickListener {

            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (view instanceof NotesListItem) {
                    NoteItemData item = ((NotesListItem) view).getItemData();
                    if (mNotesListAdapter.isInChoiceMode()) {
                        if (item.getType() == Notes.TYPE_NOTE) {
                            position = position - mNotesListView.getHeaderViewsCount();
                            mModeCallBack.onItemCheckedStateChanged(null, position, id,
                                    !mNotesListAdapter.isSelectedItem(position));
                        }
                        return;
                    }

                    switch (mState) {
                        case NOTE_LIST:
                            if (item.getType() == Notes.TYPE_FOLDER
                                    || item.getType() == Notes.TYPE_SYSTEM) {
                                openFolder(item);
                            } else if (item.getType() == Notes.TYPE_NOTE) {
                                openNode(item);
                            } else {
                                Log.e(TAG, "Wrong note type in NOTE_LIST");
                            }
                            break;
                        case SUB_FOLDER:
                        case CALL_RECORD_FOLDER:
                            if (item.getType() == Notes.TYPE_NOTE) {
                                openNode(item);
                            } else {
                                Log.e(TAG, "Wrong note type in SUB_FOLDER");
                            }
                            break;
                        default:
                            break;
                    }
                }
            }

        }

        // 启动查询目标文件夹的方法
        private void startQueryDestinationFolders() {
            String selection = NoteColumns.TYPE + "=? AND " + NoteColumns.PARENT_ID + "<>? AND " + NoteColumns.ID + "<>?";
            selection = (mState == ListEditState.NOTE_LIST) ? selection :
                    "(" + selection + ") OR (" + NoteColumns.ID + "=" + Notes.ID_ROOT_FOLDER + ")";

            mBackgroundQueryHandler.startQuery(FOLDER_LIST_QUERY_TOKEN,
                    null,
                    Notes.CONTENT_NOTE_URI,
                    FoldersListAdapter.PROJECTION,
                    selection,
                    new String[]{
                            String.valueOf(Notes.TYPE_FOLDER),
                            String.valueOf(Notes.ID_TRASH_FOLER),
                            String.valueOf(mCurrentFolderId)
                    },
                    NoteColumns.MODIFIED_DATE + " DESC");
        }

        // 处理列表项长按事件的方法
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            if (view instanceof NotesListItem) {
                mFocusNoteDataItem = ((NotesListItem) view).getItemData();
                if (mFocusNoteDataItem.getType() == Notes.TYPE_NOTE && !mNotesListAdapter.isInChoiceMode()) {
                    if (mNotesListView.startActionMode(mModeCallBack) != null) {
                        mModeCallBack.onItemCheckedStateChanged(null, position, id, true);
                        mNotesListView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                    } else {
                        Log.e(TAG, "startActionMode fails");
                    }
                } else if (mFocusNoteDataItem.getType() == Notes.TYPE_FOLDER) {
                    mNotesListView.setOnCreateContextMenuListener(mFolderOnCreateContextMenuListener);
                }
            }
            return false;
        }
    }
