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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.appwidget.AppWidgetManager;
import android.content.*;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.*;
import android.text.format.DateUtils;
import android.text.style.AlignmentSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.*;
import android.widget.CompoundButton.OnCheckedChangeListener;

import androidx.annotation.RequiresApi;
import net.micode.notes.R;
import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.TextNote;
import net.micode.notes.model.WorkingNote;
import net.micode.notes.model.WorkingNote.NoteSettingChangedListener;
import net.micode.notes.tool.DataUtils;
import net.micode.notes.tool.ResourceParser;
import net.micode.notes.tool.ResourceParser.TextAppearanceResources;
import net.micode.notes.ui.DateTimePickerDialog.OnDateTimeSetListener;
import net.micode.notes.ui.NoteEditText.OnTextViewChangeListener;
import net.micode.notes.widget.NoteWidgetProvider_2x;
import net.micode.notes.widget.NoteWidgetProvider_4x;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.*;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// NoteEditActivity类，继承自Activity，实现了OnClickListener、NoteSettingChangedListener和OnTextViewChangeListener接口
// 该类用于实现笔记编辑界面的功能
public class NoteEditActivity extends Activity implements OnClickListener,
        NoteSettingChangedListener, OnTextViewChangeListener {
    private String mPreviousContent = "";

    private class HeadViewHolder {
        public TextView tvModified; // 显示笔记修改时间的TextView
        public ImageView ivAlertIcon; // 提醒图标ImageView
        public TextView tvAlertDate; // 显示提醒日期的TextView
        public ImageView ibSetBgColor; // 设置背景颜色的ImageView
    }

    // 静态常量，用于映射背景颜色选择按钮的id和对应的背景颜色资源id
    private static final Map<Integer, Integer> sBgSelectorBtnsMap = new HashMap<Integer, Integer>();
    static {
        sBgSelectorBtnsMap.put(R.id.iv_bg_yellow, ResourceParser.YELLOW);
        sBgSelectorBtnsMap.put(R.id.iv_bg_red, ResourceParser.RED);
        sBgSelectorBtnsMap.put(R.id.iv_bg_blue, ResourceParser.BLUE);
        sBgSelectorBtnsMap.put(R.id.iv_bg_green, ResourceParser.GREEN);
        sBgSelectorBtnsMap.put(R.id.iv_bg_white, ResourceParser.WHITE);
    }

    // 静态常量，用于映射背景颜色id和对应的选中状态的背景图片id
    private static final Map<Integer, Integer> sBgSelectorSelectionMap = new HashMap<Integer, Integer>();
    static {
        sBgSelectorSelectionMap.put(ResourceParser.YELLOW, R.id.iv_bg_yellow_select);
        sBgSelectorSelectionMap.put(ResourceParser.RED, R.id.iv_bg_red_select);
        sBgSelectorSelectionMap.put(ResourceParser.BLUE, R.id.iv_bg_blue_select);
        sBgSelectorSelectionMap.put(ResourceParser.GREEN, R.id.iv_bg_green_select);
        sBgSelectorSelectionMap.put(ResourceParser.WHITE, R.id.iv_bg_white_select);
    }

    // 静态常量，用于映射字体大小选择按钮的id和对应的字体大小资源id
    private static final Map<Integer, Integer> sFontSizeBtnsMap = new HashMap<Integer, Integer>();
    static {
        sFontSizeBtnsMap.put(R.id.ll_font_large, ResourceParser.TEXT_LARGE);
        sFontSizeBtnsMap.put(R.id.ll_font_small, ResourceParser.TEXT_SMALL);
        sFontSizeBtnsMap.put(R.id.ll_font_normal, ResourceParser.TEXT_MEDIUM);
        sFontSizeBtnsMap.put(R.id.ll_font_super, ResourceParser.TEXT_SUPER);
    }

    // 静态常量，用于映射字体大小id和对应的选中状态的字体图片id
    private static final Map<Integer, Integer> sFontSelectorSelectionMap = new HashMap<Integer, Integer>();
    static {
        sFontSelectorSelectionMap.put(ResourceParser.TEXT_LARGE, R.id.iv_large_select);
        sFontSelectorSelectionMap.put(ResourceParser.TEXT_SMALL, R.id.iv_small_select);
        sFontSelectorSelectionMap.put(ResourceParser.TEXT_MEDIUM, R.id.iv_medium_select);
        sFontSelectorSelectionMap.put(ResourceParser.TEXT_SUPER, R.id.iv_super_select);
    }

    // 日志标签
    private static final String TAG = "NoteEditActivity";

    // 用于存储笔记头部视图的控件持有者
    private HeadViewHolder mNoteHeaderHolder;
    // 笔记头部视图面板
    private View mHeadViewPanel;
    // 背景颜色选择视图
    private View mNoteBgColorSelector;
    // 字体大小选择视图
    private View mFontSizeSelector;
    // 笔记编辑文本框
    private EditText mNoteEditor;
    // 笔记编辑视图面板
    private View mNoteEditorPanel;
    // 当前正在编辑的笔记对象
    private WorkingNote mWorkingNote;
    // 共享偏好设置对象，用于存储应用的设置信息
    private SharedPreferences mSharedPrefs;
    // 当前选择的字体大小id
    private int mFontSizeId;
    // 用于存储字体大小设置的偏好设置键
    private static final String PREFERENCE_FONT_SIZE = "pref_font_size";
    // 快捷方式图标标题的最大长度
    private static final int SHORTCUT_ICON_TITLE_MAX_LEN = 10;
    // 选中状态的标签字符串
    public static final String TAG_CHECKED = String.valueOf('\u221A');
    // 未选中状态的标签字符串
    public static final String TAG_UNCHECKED = String.valueOf('\u25A1');

    // 用于显示列表模式下的EditText的LinearLayout
    private LinearLayout mEditTextList;
    // 用户搜索查询的字符串
    private String mUserQuery;
    // 用于匹配搜索查询的正则表达式模式
    private Pattern mPattern;

    private final int PHOTO_REQUEST = 1;

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 设置当前Activity的布局为R.layout.note_edit
        this.setContentView(R.layout.note_edit);

        // 如果savedInstanceState为空且初始化Activity状态失败，则结束当前Activity
        if (savedInstanceState == null && !initActivityState(getIntent())) {
            finish();
            return;
        }
        // 初始化资源
        initResources();

        // 根据id获取添加图片按钮
        final ImageButton add_img_btn = (ImageButton) findViewById(R.id.add_img_btn);
        // 为点击图片按钮设置监听器
        add_img_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: click add image button");
                // ACTION_GET_CONTENT: 允许用户选择特殊种类的数据，并返回（特殊种类的数据：照一张相片或录一段音）
                Intent loadImage = new Intent(Intent.ACTION_GET_CONTENT);
                // Category属性用于指定当前动作（Action）被执行的环境.
                // CATEGORY_OPENABLE; 用来指示一个ACTION_GET_CONTENT的intent
                loadImage.addCategory(Intent.CATEGORY_OPENABLE);
                loadImage.setType("image/*");
                startActivityForResult(loadImage, PHOTO_REQUEST);
            }
        });
        // 添加 TextWatcher 监听文本变化
        mNoteEditor.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                mPreviousContent = s.toString();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 检测图片标签被删除
                detectImageDeletion(mPreviousContent, s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }

        });
    }

    private void detectImageDeletion(String oldContent, String newContent) {
        Pattern pattern = Pattern.compile("\\[local](.*?)\\[/local]");
        Matcher oldMatcher = pattern.matcher(oldContent);
        Matcher newMatcher = pattern.matcher(newContent);

        Set<String> oldPaths = new HashSet<>();
        Set<String> newPaths = new HashSet<>();

        while (oldMatcher.find()) {
            oldPaths.add(oldMatcher.group(1));
        }

        while (newMatcher.find()) {
            newPaths.add(newMatcher.group(1));
        }

        // 找到被删除的图片路径
        oldPaths.removeAll(newPaths);
        for (String path : oldPaths) {
            File imageFile = new File(path);
            if (imageFile.exists()) {
                boolean deleted = imageFile.delete();
                if (deleted) {
                    Log.d(TAG, "Deleted image file: " + path);
                } else {
                    Log.e(TAG, "Failed to delete image file: " + path);
                }
            }
        }
    }

    // 当Activity因内存不足被杀死后恢复时调用的方法
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // 如果savedInstanceState不为空且包含EXTRA_UID，则恢复之前的笔记状态
        if (savedInstanceState != null && savedInstanceState.containsKey(Intent.EXTRA_UID)) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.putExtra(Intent.EXTRA_UID, savedInstanceState.getLong(Intent.EXTRA_UID));
            if (!initActivityState(intent)) {
                finish();
                return;
            }
            Log.d(TAG, "Restoring from killed activity");
        }
    }

    // 初始化Activity状态的方法，根据传入的Intent来设置当前正在编辑的笔记
    private boolean initActivityState(Intent intent) {
        // 初始化当前正在编辑的笔记为null
        mWorkingNote = null;
        // 如果Intent的动作是ACTION_VIEW，即查看已有的笔记
        if (TextUtils.equals(Intent.ACTION_VIEW, intent.getAction())) {
            long noteId = intent.getLongExtra(Intent.EXTRA_UID, 0);
            mUserQuery = "";

            // 如果Intent包含搜索结果的额外数据，则更新noteId和mUserQuery
            if (intent.hasExtra(SearchManager.EXTRA_DATA_KEY)) {
                noteId = Long.parseLong(intent.getStringExtra(SearchManager.EXTRA_DATA_KEY));
                mUserQuery = intent.getStringExtra(SearchManager.USER_QUERY);
            }

            // 如果笔记在数据库中不可见，则跳转到NotesListActivity并显示错误提示
            if (!DataUtils.visibleInNoteDatabase(getContentResolver(), noteId, Notes.TYPE_NOTE)) {
                Intent jump = new Intent(this, NotesListActivity.class);
                startActivity(jump);
                showToast(R.string.error_note_not_exist);
                finish();
                return false;
            } else {
                // 从数据库加载笔记
                mWorkingNote = WorkingNote.load(this, noteId);
                if (mWorkingNote == null) {
                    Log.e(TAG, "load note failed with note id" + noteId);
                    finish();
                    return false;
                }
            }
            // 设置软键盘的显示模式
            getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
                            | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        } else if (TextUtils.equals(Intent.ACTION_INSERT_OR_EDIT, intent.getAction())) {
            // 新建笔记的情况
            long folderId = intent.getLongExtra(Notes.INTENT_EXTRA_FOLDER_ID, 0);
            int widgetId = intent.getIntExtra(Notes.INTENT_EXTRA_WIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
            int widgetType = intent.getIntExtra(Notes.INTENT_EXTRA_WIDGET_TYPE,
                    Notes.TYPE_WIDGET_INVALIDE);
            int bgResId = intent.getIntExtra(Notes.INTENT_EXTRA_BACKGROUND_ID,
                    ResourceParser.getDefaultBgId(this));

            // 解析通话记录笔记
            String phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
            long callDate = intent.getLongExtra(Notes.INTENT_EXTRA_CALL_DATE, 0);
            if (callDate != 0 && phoneNumber != null) {
                if (TextUtils.isEmpty(phoneNumber)) {
                    Log.w(TAG, "The call record number is null");
                }
                long noteId = 0;
                // 如果根据电话号码和通话日期能找到笔记，则加载该笔记
                if ((noteId = DataUtils.getNoteIdByPhoneNumberAndCallDate(getContentResolver(),
                        phoneNumber, callDate)) > 0) {
                    mWorkingNote = WorkingNote.load(this, noteId);
                    if (mWorkingNote == null) {
                        Log.e(TAG, "load call note failed with note id" + noteId);
                        finish();
                        return false;
                    }
                } else {
                    // 否则创建一个新的通话记录笔记
                    mWorkingNote = WorkingNote.createEmptyNote(this, folderId, widgetId,
                            widgetType, bgResId);
                    mWorkingNote.convertToCallNote(phoneNumber, callDate);
                }
            } else {
                // 创建一个新的普通笔记
                mWorkingNote = WorkingNote.createEmptyNote(this, folderId, widgetId, widgetType,
                        bgResId);
            }

            // 设置软键盘的显示模式
            getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                            | WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        } else {
            // 如果Intent的动作不支持，则记录错误并结束当前Activity
            Log.e(TAG, "Intent not specified action, should not support");
            finish();
            return false;
        }
        // 设置笔记设置状态改变的监听器
        mWorkingNote.setOnSettingStatusChangedListener(this);
        return true;
    }

    // Activity恢复时调用的方法
    @Override
    protected void onResume() {
        super.onResume();
        // 初始化笔记编辑界面
        initNoteScreen();
        convertToImage();
    }

    // 初始化笔记编辑界面的方法
    private void initNoteScreen() {
        // 设置笔记编辑文本框的文本外观，根据当前选择的字体大小
        mNoteEditor.setTextAppearance(this, TextAppearanceResources
                .getTexAppearanceResource(mFontSizeId));
        // 如果笔记是 checklist 模式，则切换到列表模式并设置内容
        if (mWorkingNote.getCheckListMode() == TextNote.MODE_CHECK_LIST) {
            switchToListMode(mWorkingNote.getContent());
        } else {
            // 否则设置笔记内容并高亮显示搜索查询结果
            mNoteEditor.setText(getHighlightQueryResult(mWorkingNote.getContent(), mUserQuery));
            mNoteEditor.setSelection(mNoteEditor.getText().length());
        }
        // 隐藏所有背景颜色选择的选中状态视图
        for (Integer id : sBgSelectorSelectionMap.keySet()) {
            findViewById(sBgSelectorSelectionMap.get(id)).setVisibility(View.GONE);
        }
        // 设置笔记头部视图和编辑视图的背景资源
        mHeadViewPanel.setBackgroundResource(mWorkingNote.getTitleBgResId());
        mNoteEditorPanel.setBackgroundResource(mWorkingNote.getBgColorResId());

        // 设置笔记修改时间的文本
        mNoteHeaderHolder.tvModified.setText(DateUtils.formatDateTime(this,
                mWorkingNote.getModifiedDate(), DateUtils.FORMAT_SHOW_DATE
                        | DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_TIME
                        | DateUtils.FORMAT_SHOW_YEAR));

        // 显示提醒头部信息（TODO：目前禁用，因为DateTimePicker未准备好）
        showAlertHeader();

    }

    // 显示提醒头部信息的方法
    private void showAlertHeader() {
        // 如果笔记有提醒时间
        if (mWorkingNote.hasClockAlert()) {
            long time = System.currentTimeMillis();
            // 如果当前时间大于提醒时间，则显示提醒已过期
            if (time > mWorkingNote.getAlertDate()) {
                mNoteHeaderHolder.tvAlertDate.setText(R.string.note_alert_expired);
            } else {
                // 否则显示距离提醒时间的相对时间
                mNoteHeaderHolder.tvAlertDate.setText(DateUtils.getRelativeTimeSpanString(
                        mWorkingNote.getAlertDate(), time, DateUtils.MINUTE_IN_MILLIS));
            }
            // 显示提醒日期和图标
            mNoteHeaderHolder.tvAlertDate.setVisibility(View.VISIBLE);
            mNoteHeaderHolder.ivAlertIcon.setVisibility(View.VISIBLE);
        } else {
            // 如果没有提醒时间，则隐藏提醒日期和图标
            mNoteHeaderHolder.tvAlertDate.setVisibility(View.GONE);
            mNoteHeaderHolder.ivAlertIcon.setVisibility(View.GONE);
        }
    }

    // 当Activity接收到新的Intent时调用的方法
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // 重新初始化Activity状态
        initActivityState(intent);
    }

    // 当Activity被暂停时保存状态的方法
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // 在创建一个新的标签时，先在数据库中匹配
        // 如果不存在，那么先在数据库中存储
        /**
         * For new note without note id, we should firstly save it to
         * generate a id. If the editing note is not worth saving, there
         * is no id which is equivalent to create new note
         */
        if (!mWorkingNote.existInDatabase()) {
            saveNote();
        }
        // 在创建一个新的标签时，先在数据库中匹配
        // 如果不存在，那么先在数据库中存储
        outState.putLong(Intent.EXTRA_UID, mWorkingNote.getNoteId());
        Log.d(TAG, "Save working note id: " + mWorkingNote.getNoteId() + " onSaveInstanceState");
    }

    // 分发触摸事件的方法
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        // 如果背景颜色选择视图可见且触摸点不在该视图范围内，则隐藏该视图
        if (mNoteBgColorSelector.getVisibility() == View.VISIBLE
                && !inRangeOfView(mNoteBgColorSelector, ev)) {
            mNoteBgColorSelector.setVisibility(View.GONE);
            return true;
        }

        // 如果字体大小选择视图可见且触摸点不在该视图范围内，则隐藏该视图
        if (mFontSizeSelector.getVisibility() == View.VISIBLE
                && !inRangeOfView(mFontSizeSelector, ev)) {
            mFontSizeSelector.setVisibility(View.GONE);
            return true;
        }
        // 否则调用父类的dispatchTouchEvent方法处理触摸事件
        return super.dispatchTouchEvent(ev);
    }

    // 判断触摸点是否在指定视图范围内的方法
    private boolean inRangeOfView(View view, MotionEvent ev) {
        int[] location = new int[2];
        // 获取视图在屏幕上的位置
        view.getLocationOnScreen(location);
        int x = location[0];
        int y = location[1];
        // 判断触摸点是否在视图范围内
        if (ev.getX() < x
                || ev.getX() > (x + view.getWidth())
                || ev.getY() < y
                || ev.getY() > (y + view.getHeight())) {
            return false;
        }
        return true;
    }

    // 初始化资源的方法
    private void initResources() {
        // 获取笔记标题视图
        mHeadViewPanel = findViewById(R.id.note_title);
        mNoteHeaderHolder = new HeadViewHolder();
        // 获取显示修改时间的TextView
        mNoteHeaderHolder.tvModified = (TextView) findViewById(R.id.tv_modified_date);
        // 获取提醒图标ImageView
        mNoteHeaderHolder.ivAlertIcon = (ImageView) findViewById(R.id.iv_alert_icon);
        // 获取显示提醒日期的TextView
        mNoteHeaderHolder.tvAlertDate = (TextView) findViewById(R.id.tv_alert_date);
        // 获取设置背景颜色的ImageView，并设置点击监听器
        mNoteHeaderHolder.ibSetBgColor = (ImageView) findViewById(R.id.btn_set_bg_color);
        mNoteHeaderHolder.ibSetBgColor.setOnClickListener(this);
        // 获取笔记编辑文本框
        mNoteEditor = (EditText) findViewById(R.id.note_edit_view);
        // 获取笔记编辑视图面板
        mNoteEditorPanel = findViewById(R.id.sv_note_edit);
        // 获取背景颜色选择视图
        mNoteBgColorSelector = findViewById(R.id.note_bg_color_selector);
        // 为背景颜色选择按钮设置点击监听器
        for (int id : sBgSelectorBtnsMap.keySet()) {
            ImageView iv = (ImageView) findViewById(id);
            iv.setOnClickListener(this);
        }

        // 获取字体大小选择视图
        mFontSizeSelector = findViewById(R.id.font_size_selector);
        // 为字体大小选择按钮设置点击监听器
        for (int id : sFontSizeBtnsMap.keySet()) {
            View view = findViewById(id);
            view.setOnClickListener(this);
        }
        // 获取默认的共享偏好设置对象
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        // 从共享偏好设置中获取当前字体大小id
        mFontSizeId = mSharedPrefs.getInt(PREFERENCE_FONT_SIZE, ResourceParser.BG_DEFAULT_FONT_SIZE);
        // 如果字体大小id超出了资源范围，则设置为默认字体大小
        if (mFontSizeId >= TextAppearanceResources.getResourcesSize()) {
            mFontSizeId = ResourceParser.BG_DEFAULT_FONT_SIZE;
        }
        // 获取用于显示列表模式下EditText的LinearLayout
        mEditTextList = (LinearLayout) findViewById(R.id.note_edit_list);
    }

    // Activity暂停时调用的方法
    @Override
    protected void onPause() {
        super.onPause();
        // 保存笔记，如果保存成功则记录日志
        if (saveNote()) {
            Log.d(TAG, "Note data was saved with length:" + mWorkingNote.getContent().length());
        }
        // 清除设置状态
        clearSettingState();
    }

    // 更新桌面小部件的方法
    private void updateWidget() {
        Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        // 根据笔记的小部件类型设置Intent的类
        if (mWorkingNote.getWidgetType() == Notes.TYPE_WIDGET_2X) {
            intent.setClass(this, NoteWidgetProvider_2x.class);
        } else if (mWorkingNote.getWidgetType() == Notes.TYPE_WIDGET_4X) {
            intent.setClass(this, NoteWidgetProvider_4x.class);
        } else {
            Log.e(TAG, "Unspported widget type");
            return;
        }

        // 设置小部件id数组
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[] {
                mWorkingNote.getWidgetId()
        });

        // 发送广播更新小部件，并设置结果为RESULT_OK
        sendBroadcast(intent);
        setResult(RESULT_OK, intent);
    }

    // 点击事件处理方法
    @Override
    public void onClick(View v) {
        int id = v.getId();
        // 如果点击的是设置背景颜色按钮
        if (id == R.id.btn_set_bg_color) {
            // 显示背景颜色选择视图，并显示当前选中的背景颜色的选中状态视图
            mNoteBgColorSelector.setVisibility(View.VISIBLE);
            findViewById(sBgSelectorSelectionMap.get(mWorkingNote.getBgColorId())).setVisibility(
                    View.VISIBLE);
        } else if (sBgSelectorBtnsMap.containsKey(id)) {
            // 如果点击的是背景颜色选择按钮
            // 隐藏当前选中的背景颜色的选中状态视图
            findViewById(sBgSelectorSelectionMap.get(mWorkingNote.getBgColorId())).setVisibility(
                    View.GONE);
            // 设置新的背景颜色id
            mWorkingNote.setBgColorId(sBgSelectorBtnsMap.get(id));
            // 隐藏背景颜色选择视图
            mNoteBgColorSelector.setVisibility(View.GONE);
        } else if (sFontSizeBtnsMap.containsKey(id)) {
            // 如果点击的是字体大小选择按钮
            // 隐藏当前选中的字体大小的选中状态视图
            findViewById(sFontSelectorSelectionMap.get(mFontSizeId)).setVisibility(View.GONE);
            // 设置新的字体大小id，并保存到共享偏好设置中
            mFontSizeId = sFontSizeBtnsMap.get(id);
            mSharedPrefs.edit().putInt(PREFERENCE_FONT_SIZE, mFontSizeId).commit();
            // 显示新选中的字体大小的选中状态视图
            findViewById(sFontSelectorSelectionMap.get(mFontSizeId)).setVisibility(View.VISIBLE);
            // 如果笔记是 checklist 模式，则切换到列表模式并设置内容
            if (mWorkingNote.getCheckListMode() == TextNote.MODE_CHECK_LIST) {
                getWorkingText();
                switchToListMode(mWorkingNote.getContent());
            } else {
                // 否则设置笔记编辑文本框的文本外观
                mNoteEditor.setTextAppearance(this,
                        TextAppearanceResources.getTexAppearanceResource(mFontSizeId));
            }
            // 隐藏字体大小选择视图
            mFontSizeSelector.setVisibility(View.GONE);
        }
    }

    // 按下返回键时调用的方法
    @Override
    public void onBackPressed() {
        // 如果可以清除设置状态，则返回
        if (clearSettingState()) {
            return;
        }

        // 保存笔记，然后调用父类的onBackPressed方法
        saveNote();
        super.onBackPressed();
    }

    // 清除设置状态的方法
    private boolean clearSettingState() {
        // 如果背景颜色选择视图可见，则隐藏该视图并返回true
        if (mNoteBgColorSelector.getVisibility() == View.VISIBLE) {
            mNoteBgColorSelector.setVisibility(View.GONE);
            return true;
        } else if (mFontSizeSelector.getVisibility() == View.VISIBLE) {
            // 如果字体大小选择视图可见，则隐藏该视图并返回true
            mFontSizeSelector.setVisibility(View.GONE);
            return true;
        }
        return false;
    }

    // 当背景颜色改变时调用的方法
    public void onBackgroundColorChanged() {
        // 显示当前选中的背景颜色的选中状态视图
        findViewById(sBgSelectorSelectionMap.get(mWorkingNote.getBgColorId())).setVisibility(
                View.VISIBLE);
        // 设置笔记编辑视图和头部视图的背景资源
        mNoteEditorPanel.setBackgroundResource(mWorkingNote.getBgColorResId());
        mHeadViewPanel.setBackgroundResource(mWorkingNote.getTitleBgResId());
    }

    // 准备选项菜单时调用的方法
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // 如果Activity正在结束，则返回true
        if (isFinishing()) {
            return true;
        }
        // 清除设置状态
        clearSettingState();
        // 清除菜单内容
        menu.clear();
        // 根据笔记所在文件夹类型加载不同的菜单布局
        if (mWorkingNote.getFolderId() == Notes.ID_CALL_RECORD_FOLDER) {
            getMenuInflater().inflate(R.menu.call_note_edit, menu);
        } else {
            getMenuInflater().inflate(R.menu.note_edit, menu);
        }
        // 根据笔记的 checklist 模式设置列表模式菜单项的标题
        if (mWorkingNote.getCheckListMode() == TextNote.MODE_CHECK_LIST) {
            menu.findItem(R.id.menu_list_mode).setTitle(R.string.menu_normal_mode);
        } else {
            menu.findItem(R.id.menu_list_mode).setTitle(R.string.menu_list_mode);
        }
        // 根据笔记是否有提醒时间设置提醒相关菜单项的可见性
        if (mWorkingNote.hasClockAlert()) {
            menu.findItem(R.id.menu_alert).setVisible(false);
        } else {
            menu.findItem(R.id.menu_delete_remind).setVisible(false);
        }
        return true;
    }

    // 选项菜单项点击事件处理方法
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_new_note:
                // 创建新笔记
                createNewNote();
                break;
            case R.id.menu_delete:
                // 创建删除笔记的确认对话框
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(getString(R.string.alert_title_delete));
                builder.setIcon(android.R.drawable.ic_dialog_alert);
                builder.setMessage(getString(R.string.alert_message_delete_note));
                builder.setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // 删除当前笔记并结束Activity
                                deleteCurrentNote();
                                finish();
                            }
                        });
                builder.setNegativeButton(android.R.string.cancel, null);
                builder.show();
                break;
            case R.id.menu_font_size:
                // 显示字体大小选择视图，并显示当前选中的字体大小的选中状态视图
                mFontSizeSelector.setVisibility(View.VISIBLE);
                findViewById(sFontSelectorSelectionMap.get(mFontSizeId)).setVisibility(View.VISIBLE);
                break;
            case R.id.menu_list_mode:
                // 切换笔记的 checklist 模式
                mWorkingNote.setCheckListMode(mWorkingNote.getCheckListMode() == 0 ? TextNote.MODE_CHECK_LIST : 0);
                break;
            case R.id.menu_share:
                // 获取笔记内容并分享
                getWorkingText();
                sendTo(this, mWorkingNote.getContent());
                break;
            case R.id.menu_send_to_desktop:
                // 将笔记发送到桌面创建快捷方式
                sendToDesktop();
                break;
            case R.id.menu_alert:
                // 设置提醒时间
                setReminder();
                break;
            case R.id.menu_delete_remind:
                // 取消提醒时间
                mWorkingNote.setAlertDate(0, false);
                break;
            default:
                break;
        }
        return true;
    }

    // 设置提醒时间的方法
    private void setReminder() {
        // 创建日期时间选择对话框
        DateTimePickerDialog d = new DateTimePickerDialog(this, System.currentTimeMillis());
        // 设置日期时间选择监听器
        d.setOnDateTimeSetListener(new OnDateTimeSetListener() {
            public void OnDateTimeSet(AlertDialog dialog, long date) {
                // 设置笔记的提醒时间
                mWorkingNote.setAlertDate(date, true);
            }
        });
        // 显示对话框
        d.show();
    }

    // 分享笔记的方法
    private void sendTo(Context context, String info) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        // 设置分享的文本内容
        intent.putExtra(Intent.EXTRA_TEXT, info);
        // 设置分享的类型为纯文本
        intent.setType("text/plain");
        // 启动分享活动
        context.startActivity(intent);
    }

    // 创建新笔记的方法
    private void createNewNote() {
        // 首先保存当前正在编辑的笔记
        saveNote();

        // 结束当前Activity并启动新的NoteEditActivity创建新笔记
        finish();
        Intent intent = new Intent(this, NoteEditActivity.class);
        intent.setAction(Intent.ACTION_INSERT_OR_EDIT);
        intent.putExtra(Notes.INTENT_EXTRA_FOLDER_ID, mWorkingNote.getFolderId());
        startActivity(intent);
    }

    // 删除当前笔记的方法
    private void deleteCurrentNote() {
        // 如果笔记在数据库中存在
        if (mWorkingNote.existInDatabase()) {
            HashSet<Long> ids = new HashSet<Long>();
            long id = mWorkingNote.getNoteId();
            // 如果笔记id不是根文件夹id，则添加到要删除的id集合中
            if (id != Notes.ID_ROOT_FOLDER) {
                ids.add(id);
            } else {
                Log.d(TAG, "Wrong note id, should not happen");
            }
            // 如果不是同步模式，则直接从数据库中删除笔记
            if (!isSyncMode()) {
                if (!DataUtils.batchDeleteNotes(getContentResolver(), ids)) {
                    Log.e(TAG, "Delete Note error");
                }
            } else {
                // 如果是同步模式，则将笔记移动到回收站文件夹
                if (!DataUtils.batchMoveToFolder(getContentResolver(), ids, Notes.ID_TRASH_FOLER)) {
                    Log.e(TAG, "Move notes to trash folder error, should not happens");
                }
            }
        }
        // 将笔记标记为已删除
        mWorkingNote.markDeleted(true);
        // 删除便签中的图片文件
        deleteImagesInNote(mWorkingNote.getContent());
    }

    private void deleteImagesInNote(String noteContent) {
        Pattern pattern = Pattern.compile("\\[local](.*?)\\[/local]");
        Matcher matcher = pattern.matcher(noteContent);

        while (matcher.find()) {
            String imagePath = matcher.group(1);
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                boolean deleted = imageFile.delete();
                if (deleted) {
                    Log.d(TAG, "Deleted image file: " + imagePath);
                } else {
                    Log.e(TAG, "Failed to delete image file: " + imagePath);
                }
            }
        }
    }

    private boolean isSyncMode() {
        // 如果同步账户名称不为空，则返回true
        return NotesPreferenceActivity.getSyncAccountName(this).trim().length() > 0;
    }

    // 当提醒时间改变时调用的方法
    public void onClockAlertChanged(long date, boolean set) {
        // 如果笔记在数据库中不存在，则先保存笔记
        if (!mWorkingNote.existInDatabase()) {
            saveNote();
        }
        // 如果笔记id大于0，则设置提醒闹钟
        if (mWorkingNote.getNoteId() > 0) {
            Intent intent = new Intent(this, AlarmReceiver.class);
            intent.setData(ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, mWorkingNote.getNoteId()));
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
            AlarmManager alarmManager = ((AlarmManager) getSystemService(ALARM_SERVICE));
            // 显示提醒头部信息
            showAlertHeader();
            // 如果取消设置提醒，则取消闹钟
            if (!set) {
                alarmManager.cancel(pendingIntent);
            } else {
                // 如果设置提醒，则设置闹钟
                alarmManager.set(AlarmManager.RTC_WAKEUP, date, pendingIntent);
            }
        } else {
            // 如果笔记id不大于0，则记录错误并显示提示信息
            Log.e(TAG, "Clock alert setting error");
            showToast(R.string.error_note_empty_for_clock);
        }
    }

    // 当小部件改变时调用的方法
    public void onWidgetChanged() {
        // 更新桌面小部件
        updateWidget();
    }

    // 当列表模式下的EditText删除时调用的方法
    public void onEditTextDelete(int index, String text) {
        int childCount = mEditTextList.getChildCount();
        // 如果列表中只有一个子项，不做处理
        if (childCount == 1) {
            return;
        }

        // 从删除位置的下一项开始，更新后续EditText的索引
        for (int i = index + 1; i < childCount; i++) {
            ((NoteEditText) mEditTextList.getChildAt(i).findViewById(R.id.et_edit_text))
                    .setIndex(i - 1);
        }

        // 移除指定索引位置的视图
        mEditTextList.removeViewAt(index);
        NoteEditText edit = null;
        if (index == 0) {
            // 如果删除的是第一个子项，获取当前第一个子项的EditText
            edit = (NoteEditText) mEditTextList.getChildAt(0).findViewById(R.id.et_edit_text);
        } else {
            // 否则获取删除位置前一个子项的EditText
            edit = (NoteEditText) mEditTextList.getChildAt(index - 1).findViewById(R.id.et_edit_text);
        }
        int length = edit.length();
        // 将删除的文本追加到获取的EditText中
        edit.append(text);
        // 让该EditText获取焦点
        edit.requestFocus();
        // 设置光标位置在追加文本之后
        edit.setSelection(length);
    }

    // 当列表模式下的EditText输入回车时调用的方法
    public void onEditTextEnter(int index, String text) {
        // 检查索引是否超出列表范围，若超出则记录错误日志
        if (index > mEditTextList.getChildCount()) {
            Log.e(TAG, "Index out of mEditTextList boundrary, should not happen");
        }

        // 获取一个新的列表项视图
        View view = getListItem(text, index);
        // 在指定索引位置插入新的列表项视图
        mEditTextList.addView(view, index);
        NoteEditText edit = (NoteEditText) view.findViewById(R.id.et_edit_text);
        // 让新插入的EditText获取焦点
        edit.requestFocus();
        // 设置光标位置在文本开头
        edit.setSelection(0);
        // 从插入位置的下一项开始，更新后续EditText的索引
        for (int i = index + 1; i < mEditTextList.getChildCount(); i++) {
            ((NoteEditText) mEditTextList.getChildAt(i).findViewById(R.id.et_edit_text))
                    .setIndex(i);
        }
    }

    // 切换到列表模式的方法
    private void switchToListMode(String text) {
        // 清空列表中的所有视图
        mEditTextList.removeAllViews();
        // 按换行符分割文本为多个条目
        String[] items = text.split("\n");
        int index = 0;
        for (String item : items) {
            // 若条目不为空，添加到列表中
            if (!TextUtils.isEmpty(item)) {
                mEditTextList.addView(getListItem(item, index));
                index++;
            }
        }
        // 添加一个空的列表项
        mEditTextList.addView(getListItem("", index));
        // 让最后一个列表项的EditText获取焦点
        mEditTextList.getChildAt(index).findViewById(R.id.et_edit_text).requestFocus();

        // 隐藏普通编辑文本框
        mNoteEditor.setVisibility(View.GONE);
        // 显示列表模式的编辑视图
        mEditTextList.setVisibility(View.VISIBLE);
    }

    // 高亮显示搜索结果的方法
    private Spannable getHighlightQueryResult(String fullText, String userQuery) {
        // 创建一个SpannableString对象，用于存储文本
        SpannableString spannable = new SpannableString(fullText == null ? "" : fullText);
        if (!TextUtils.isEmpty(userQuery)) {
            // 编译正则表达式模式
            mPattern = Pattern.compile(userQuery);
            Matcher m = mPattern.matcher(fullText);
            int start = 0;
            while (m.find(start)) {
                // 为匹配到的文本添加背景颜色高亮
                spannable.setSpan(
                        new BackgroundColorSpan(this.getResources().getColor(R.color.user_query_highlight)),
                        m.start(), m.end(),
                        Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                start = m.end();
            }
        }
        return spannable;
    }

    // 获取列表项视图的方法
    private View getListItem(String item, int index) {
        // 从布局文件中加载列表项视图
        View view = LayoutInflater.from(this).inflate(R.layout.note_edit_list_item, null);
        final NoteEditText edit = (NoteEditText) view.findViewById(R.id.et_edit_text);
        // 设置EditText的文本外观
        edit.setTextAppearance(this, TextAppearanceResources.getTexAppearanceResource(mFontSizeId));
        CheckBox cb = ((CheckBox) view.findViewById(R.id.cb_edit_item));
        // 为CheckBox设置选中状态改变监听器
        cb.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // 若选中，为EditText文本添加删除线
                    edit.setPaintFlags(edit.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                } else {
                    // 若未选中，移除删除线
                    edit.setPaintFlags(Paint.ANTI_ALIAS_FLAG | Paint.DEV_KERN_TEXT_FLAG);
                }
            }
        });

        if (item.startsWith(TAG_CHECKED)) {
            // 若文本以选中标签开头，设置CheckBox为选中状态并添加删除线
            cb.setChecked(true);
            edit.setPaintFlags(edit.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            item = item.substring(TAG_CHECKED.length(), item.length()).trim();
        } else if (item.startsWith(TAG_UNCHECKED)) {
            // 若文本以未选中标签开头，设置CheckBox为未选中状态并移除删除线
            cb.setChecked(false);
            edit.setPaintFlags(Paint.ANTI_ALIAS_FLAG | Paint.DEV_KERN_TEXT_FLAG);
            item = item.substring(TAG_UNCHECKED.length(), item.length()).trim();
        }

        // 为EditText设置文本变化监听器
        edit.setOnTextViewChangeListener(this);
        // 设置EditText的索引
        edit.setIndex(index);
        // 设置EditText的文本并高亮显示搜索结果
        edit.setText(getHighlightQueryResult(item, mUserQuery));
        return view;
    }

    // 当列表模式下的EditText文本变化时调用的方法
    public void onTextChange(int index, boolean hasText) {
        // 检查索引是否超出列表范围，若超出则记录错误日志
        if (index >= mEditTextList.getChildCount()) {
            Log.e(TAG, "Wrong index, should not happen");
            return;
        }
        if (hasText) {
            // 若EditText有文本，显示对应的CheckBox
            mEditTextList.getChildAt(index).findViewById(R.id.cb_edit_item).setVisibility(View.VISIBLE);
        } else {
            // 若EditText无文本，隐藏对应的CheckBox
            mEditTextList.getChildAt(index).findViewById(R.id.cb_edit_item).setVisibility(View.GONE);
        }
        // 更新 mPreviousContent 的值
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < mEditTextList.getChildCount(); i++) {
            View view = mEditTextList.getChildAt(i);
            NoteEditText edit = (NoteEditText) view.findViewById(R.id.et_edit_text);
            s.append(edit.getText()).append("\n");
        }
        mPreviousContent = s.toString(); // 正确解析 mPreviousContent
    }

    // 当笔记的列表模式改变时调用的方法
    public void onCheckListModeChanged(int oldMode, int newMode) {
        if (newMode == TextNote.MODE_CHECK_LIST) {
            // 若切换到列表模式，将普通编辑文本框的内容转换为列表模式显示
            switchToListMode(mNoteEditor.getText().toString());
        } else {
            // 若切换到普通模式，获取列表模式下的文本内容
            if (!getWorkingText()) {
                // 若获取失败，去除未选中标签并设置为笔记内容
                mWorkingNote.setWorkingText(mWorkingNote.getContent().replace(TAG_UNCHECKED + " ", ""));
            }
            // 设置普通编辑文本框的文本并高亮显示搜索结果
            mNoteEditor.setText(getHighlightQueryResult(mWorkingNote.getContent(), mUserQuery));
            // 隐藏列表模式的编辑视图
            mEditTextList.setVisibility(View.GONE);
            // 显示普通编辑文本框
            mNoteEditor.setVisibility(View.VISIBLE);
            convertToImage();
        }

    }

    // 获取当前编辑的文本内容的方法
    private boolean getWorkingText() {
        boolean hasChecked = false;
        if (mWorkingNote.getCheckListMode() == TextNote.MODE_CHECK_LIST) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < mEditTextList.getChildCount(); i++) {
                View view = mEditTextList.getChildAt(i);
                NoteEditText edit = (NoteEditText) view.findViewById(R.id.et_edit_text);
                if (!TextUtils.isEmpty(edit.getText())) {
                    CheckBox cb = (CheckBox) view.findViewById(R.id.cb_edit_item);
                    if (cb.isChecked()) {
                        // 若CheckBox选中，添加选中标签和文本
                        sb.append(TAG_CHECKED).append(" ").append(edit.getText()).append("\n");
                        hasChecked = true;
                    } else {
                        // 若CheckBox未选中，添加未选中标签和文本
                        sb.append(TAG_UNCHECKED).append(" ").append(edit.getText()).append("\n");
                    }
                }
            }
            // 设置工作笔记的文本内容
            mWorkingNote.setWorkingText(sb.toString());
        } else {
            // 若为普通模式，直接设置工作笔记的文本内容为普通编辑文本框的内容
            mWorkingNote.setWorkingText(mNoteEditor.getText().toString());
        }
        return hasChecked;
    }

    // 保存笔记的方法
    private boolean saveNote() {
        // 获取当前编辑的文本内容
        getWorkingText();
        // 调用工作笔记的保存方法
        boolean saved = mWorkingNote.saveNote();
        if (saved) {
            // 若保存成功，设置Activity结果为RESULT_OK
            setResult(RESULT_OK);
        }
        return saved;
    }

    // 将笔记发送到桌面创建快捷方式的方法
    private void sendToDesktop() {
        // 若笔记在数据库中不存在，先保存笔记
        if (!mWorkingNote.existInDatabase()) {
            saveNote();
        }

        if (mWorkingNote.getNoteId() > 0) {
            Intent sender = new Intent();
            Intent shortcutIntent = new Intent(this, NoteEditActivity.class);
            shortcutIntent.setAction(Intent.ACTION_VIEW);
            shortcutIntent.putExtra(Intent.EXTRA_UID, mWorkingNote.getNoteId());
            // 设置快捷方式的启动意图
            sender.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
            // 设置快捷方式的标题
            sender.putExtra(Intent.EXTRA_SHORTCUT_NAME, makeShortcutIconTitle(mWorkingNote.getContent()));
            // 设置快捷方式的图标资源
            sender.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                    Intent.ShortcutIconResource.fromContext(this, R.drawable.icon_app));
            sender.putExtra("duplicate", true);
            sender.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
            // 显示提示信息
            showToast(R.string.info_note_enter_desktop);
            // 发送广播创建快捷方式
            sendBroadcast(sender);
        } else {
            // 若笔记id不合法，记录错误日志并显示提示信息
            Log.e(TAG, "Send to desktop error");
            showToast(R.string.error_note_empty_for_send_to_desktop);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public String getPath(final Context context, final Uri uri) {
        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                }
                final String selection = "_id=?";
                final String[] selectionArgs = new String[] { split[1] };
                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // Media
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    <<<<<<<HEAD

    // 获取数据列_获取此 Uri 的数据列的值。这对MediaStore Uris 和其他基于文件的 ContentProvider。
    public String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = { column };
        try {
            try {
                cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            } catch (Exception e) {

            }
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;

    }

    // 是否为媒体文件
    public boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        ContentResolver resolver = getContentResolver();
        switch (requestCode) {
            case PHOTO_REQUEST:
                Uri originalUri = intent.getData(); // 1.获得图片的真实路径
                Bitmap bitmap = null;
                try {
                    bitmap = BitmapFactory.decodeStream(resolver.openInputStream(originalUri));// 2.解码图片
                } catch (FileNotFoundException e) {
                    Log.d(TAG, "onActivityResult: get file_exception");
                    e.printStackTrace();
                }

                if (bitmap != null) {
                    // 3.根据Bitmap对象创建ImageSpan对象
                    Log.d(TAG, "onActivityResult: bitmap is not null");
                    ImageSpan imageSpan = new ImageSpan(NoteEditActivity.this, bitmap);
                    String path = getPath(this, originalUri);
                    // 4.使用[local][/local]将path括起来，用于之后方便识别图片路径在note中的位置

                    // 复制图片到应用私有目录
                    String newPath = saveImageToAppSpecificStorage(bitmap);
                    if (newPath != null) {
                        path = newPath;
                    } else {
                        Log.e(TAG, "Failed to save image to app specific storage");
                    }

                    String img_fragment = "[local]" + path + "[/local]";
                    // 创建一个SpannableString对象，以便插入用ImageSpan对象封装的图像
                    SpannableString spannableString = new SpannableString(img_fragment);
                    spannableString.setSpan(imageSpan, 0, img_fragment.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    // 5.将选择的图片追加到EditText中光标所在位置
                    NoteEditText e = (NoteEditText) findViewById(R.id.note_edit_view);
                    int index = e.getSelectionStart(); // 获取光标所在位置
                    Log.d(TAG, "Index是: " + index);
                    Editable edit_text = e.getEditableText();
                    edit_text.insert(index, spannableString); // 将图片插入到光标所在位置

                    mWorkingNote.mContent = e.getText().toString();
                    // 6.把改动提交到数据库中,两个数据库表都要改的
                    ContentResolver contentResolver = getContentResolver();
                    ContentValues contentValues = new ContentValues();
                    final long id = mWorkingNote.getNoteId();
                    contentValues.put("snippet", mWorkingNote.mContent);
                    contentResolver.update(Uri.parse("content://micode_notes/note"), contentValues, "_id=?",
                            new String[] { "" + id });
                    ContentValues contentValues1 = new ContentValues();
                    contentValues1.put("content", mWorkingNote.mContent);
                    contentResolver.update(Uri.parse("content://micode_notes/data"), contentValues1,
                            "mime_type=? and note_id=?", new String[] { "vnd.android.cursor.item/text_note", "" + id });

                } else {
                    Toast.makeText(NoteEditActivity.this, "获取图片失败", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
    }

    private String saveImageToAppSpecificStorage(Bitmap bitmap) {
        File appSpecificStorageDir = getAppSpecificStorageDir(this, "notes_images");
        File newFile = new File(appSpecificStorageDir, "note_image_" + System.currentTimeMillis() + ".jpg");
        try {
            FileOutputStream out = new FileOutputStream(newFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
            return newFile.getAbsolutePath();
        } catch (Exception e) {
            Log.e(TAG, "Error saving image", e);
        }
        return null;
    }

    private void convertToImage() {
        final NoteEditText noteEditText = findViewById(R.id.note_edit_view);
        noteEditText.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                noteEditText.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                Editable editable = noteEditText.getEditableText();

                String noteText = editable.toString();
                int cursorPositionBeforeInsert = noteEditText.getSelectionStart();

                // 使用正则表达式匹配图片标签
                Pattern pattern = Pattern.compile("\\[local](.*?)\\[/local]");
                Matcher matcher = pattern.matcher(noteText);

                boolean inserted = false;
                while (matcher.find()) {
                    String path = matcher.group(1);

                    // 直接加载图片
                    Bitmap bitmap = loadImage(path);
                    if (bitmap != null) {
                        // 图片适配：根据控件宽度缩放图片
                        int desiredWidth = noteEditText.getWidth();
                        int originalWidth = bitmap.getWidth();
                        int originalHeight = bitmap.getHeight();
                        int desiredHeight = (originalHeight * desiredWidth) / originalWidth;

                        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, desiredWidth, desiredHeight, false);

                        // 创建 ImageSpan
                        ImageSpan imageSpan = new ImageSpan(NoteEditActivity.this, scaledBitmap);
                        String ss = "[local]" + path + "[/local]";
                        SpannableString spannableString = new SpannableString(ss);
                        spannableString.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 0, ss.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE); // 设置居中对齐
                        spannableString.setSpan(imageSpan, 0, ss.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                        // 添加 ClickableSpan，使得点击图片末尾的光标可以和图片对齐
                        ClickableSpan clickableSpan = new ClickableSpan() {
                            @Override
                            public void onClick(View view) {
                                int selectionEnd = noteEditText.getSelectionEnd();
                                if (selectionEnd == cursorPositionBeforeInsert) {
                                    noteEditText.setSelection(cursorPositionBeforeInsert + 1);
                                }
                            }
                        };
                        spannableString.setSpan(clickableSpan, ss.length(), ss.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                        // 替换原始文本
                        editable.replace(matcher.start(), matcher.end(), spannableString);

                        inserted = true;
                        noteEditText.setSelection(cursorPositionBeforeInsert);

                        // 移除多余的 ClickableSpan
                        ClickableSpan[] spans = editable.getSpans(cursorPositionBeforeInsert, cursorPositionBeforeInsert, ClickableSpan.class);
                        if (spans != null && spans.length > 0) {
                            for (ClickableSpan span : spans) {
                                editable.removeSpan(span);
                            }
                        }
                    }
                }

                if (inserted) {
                    editable.append("\n");
                    // 在插入图片后为 NoteEditText 设置触摸事件监听器
                    noteEditText.setOnTouchListener(new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            int action = event.getAction();
                            if (action == MotionEvent.ACTION_DOWN) {
                                int offset = noteEditText.getOffsetForPosition(event.getX(), event.getY());
                                ImageSpan[] imageSpans = editable.getSpans(0, editable.length(), ImageSpan.class);
                                for (ImageSpan span : imageSpans) {
                                    int start = editable.getSpanStart(span);
                                    int end = editable.getSpanEnd(span);
                                    if (offset >= start && offset <= end) {
                                        // 如果光标位于图片所在行，返回 true，表示消费了该事件
                                        return true;
                                    }
                                }
                            }
                            return false;
                        }
                    });
                }

    }});}

    private File getAppSpecificStorageDir(Context context, String dirName) {
        File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), dirName);
        if (!file.mkdirs()) {
            Log.e(TAG, "Directory not created");
        }
        return file;
    }

    private Bitmap loadImage(String path) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, options);

            final int targetWidth = ((View) findViewById(R.id.note_edit_view)).getWidth();
            options.inSampleSize = calculateInSampleSize(options, targetWidth);
            options.inJustDecodeBounds = false;
            options.inPreferredConfig = Bitmap.Config.RGB_565;

            return BitmapFactory.decodeFile(path, options);
        } catch (Exception e) {
            Log.e(TAG, "图片解码失败: " + path, e);
            return null;
        }
    }

    // 计算缩放比例
    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth) {
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (width > reqWidth) {
            final int halfWidth = width / 2;
            while ((halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }=======
    // 生成快捷方式图标的标题的方法
    >>>>>>>d16614a0e5a7ac6f06af8130f3224e526a425737

    private String makeShortcutIconTitle(String content) {
        // 去除内容中的选中和未选中标签
        content = content.replace(TAG_CHECKED, "");
        content = content.replace(TAG_UNCHECKED, "");
        // 若内容长度超过最大长度，截取前一部分作为标题
        return content.length() > SHORTCUT_ICON_TITLE_MAX_LEN ? content.substring(0, SHORTCUT_ICON_TITLE_MAX_LEN)
                : content;
    }

    // 显示短时间Toast提示信息的方法
    private void showToast(int resId) {
        showToast(resId, Toast.LENGTH_SHORT);
    }

    // 显示指定时长Toast提示信息的方法
    private void showToast(int resId, int duration) {
        Toast.makeText(this, resId, duration).show();
    }
}
