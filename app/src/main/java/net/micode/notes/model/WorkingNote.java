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

package net.micode.notes.model;

import android.appwidget.AppWidgetManager;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;

import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.CallNote;
import net.micode.notes.data.Notes.DataColumns;
import net.micode.notes.data.Notes.DataConstants;
import net.micode.notes.data.Notes.NoteColumns;
import net.micode.notes.data.Notes.TextNote;
import net.micode.notes.tool.ResourceParser.NoteBgResources;

// WorkingNote 类用于管理正在编辑的笔记，提供了创建、加载、保存笔记等功能
public class WorkingNote {
    // 封装的 Note 对象，用于管理笔记的属性和数据
    private Note mNote;
    // 笔记的 ID
    private long mNoteId;
    // 笔记的内容
    private String mContent;
    // 笔记的模式，如清单模式等
    private int mMode;
    // 笔记的提醒日期
    private long mAlertDate;
    // 笔记的修改日期
    private long mModifiedDate;
    // 笔记的背景颜色 ID
    private int mBgColorId;
    // 笔记关联的小部件 ID
    private int mWidgetId;
    // 笔记关联的小部件类型
    private int mWidgetType;
    // 笔记所属文件夹的 ID
    private long mFolderId;
    // 上下文对象，用于访问系统服务和资源
    private Context mContext;
    // 日志标签，用于在日志中标识该类的相关信息
    private static final String TAG = "WorkingNote";
    // 标记笔记是否被删除
    private boolean mIsDeleted;
    // 笔记设置变化监听器，用于监听笔记设置的变化
    private NoteSettingChangedListener mNoteSettingStatusListener;

    // 查询笔记数据时使用的投影列
    public static final String[] DATA_PROJECTION = new String[]{
            DataColumns.ID,
            DataColumns.CONTENT,
            DataColumns.MIME_TYPE,
            DataColumns.DATA1,
            DataColumns.DATA2,
            DataColumns.DATA3,
            DataColumns.DATA4,
    };

    // 查询笔记信息时使用的投影列
    public static final String[] NOTE_PROJECTION = new String[]{
            NoteColumns.PARENT_ID,
            NoteColumns.ALERTED_DATE,
            NoteColumns.BG_COLOR_ID,
            NoteColumns.WIDGET_ID,
            NoteColumns.WIDGET_TYPE,
            NoteColumns.MODIFIED_DATE
    };

    // DATA_PROJECTION 数组中 ID 列的索引
    private static final int DATA_ID_COLUMN = 0;
    // DATA_PROJECTION 数组中内容列的索引
    private static final int DATA_CONTENT_COLUMN = 1;
    // DATA_PROJECTION 数组中 MIME 类型列的索引
    private static final int DATA_MIME_TYPE_COLUMN = 2;
    // DATA_PROJECTION 数组中模式列的索引
    private static final int DATA_MODE_COLUMN = 3;
    // NOTE_PROJECTION 数组中父文件夹 ID 列的索引
    private static final int NOTE_PARENT_ID_COLUMN = 0;
    // NOTE_PROJECTION 数组中提醒日期列的索引
    private static final int NOTE_ALERTED_DATE_COLUMN = 1;
    // NOTE_PROJECTION 数组中背景颜色 ID 列的索引
    private static final int NOTE_BG_COLOR_ID_COLUMN = 2;
    // NOTE_PROJECTION 数组中小部件 ID 列的索引
    private static final int NOTE_WIDGET_ID_COLUMN = 3;
    // NOTE_PROJECTION 数组中小部件类型列的索引
    private static final int NOTE_WIDGET_TYPE_COLUMN = 4;
    // NOTE_PROJECTION 数组中修改日期列的索引
    private static final int NOTE_MODIFIED_DATE_COLUMN = 5;

    /**
     * 用于创建新笔记的构造函数
     * @param context 上下文对象
     * @param folderId 笔记所属文件夹的 ID
     */
    private WorkingNote(Context context, long folderId) {
        mContext = context;
        // 初始提醒日期为 0，表示无提醒
        mAlertDate = 0;
        // 初始修改日期为当前时间
        mModifiedDate = System.currentTimeMillis();
        mFolderId = folderId;
        mNote = new Note();
        // 新笔记 ID 初始为 0
        mNoteId = 0;
        // 初始标记为未删除
        mIsDeleted = false;
        // 初始模式为 0
        mMode = 0;
        // 初始小部件类型为无效类型
        mWidgetType = Notes.TYPE_WIDGET_INVALIDE;
    }

    /**
     * 用于加载现有笔记的构造函数
     * @param context 上下文对象
     * @param noteId 笔记的 ID
     * @param folderId 笔记所属文件夹的 ID
     */
    private WorkingNote(Context context, long noteId, long folderId) {
        mContext = context;
        mNoteId = noteId;
        mFolderId = folderId;
        // 初始标记为未删除
        mIsDeleted = false;
        mNote = new Note();
        // 加载笔记信息和数据
        loadNote();
    }

    /**
     * 加载笔记的基本信息，如所属文件夹、背景颜色、小部件信息、提醒日期和修改日期等
     */
    private void loadNote() {
        // 查询指定 ID 的笔记信息
        Cursor cursor = mContext.getContentResolver().query(
                ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, mNoteId), NOTE_PROJECTION, null,
                null, null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                // 获取笔记所属文件夹的 ID
                mFolderId = cursor.getLong(NOTE_PARENT_ID_COLUMN);
                // 获取笔记的背景颜色 ID
                mBgColorId = cursor.getInt(NOTE_BG_COLOR_ID_COLUMN);
                // 获取笔记关联的小部件 ID
                mWidgetId = cursor.getInt(NOTE_WIDGET_ID_COLUMN);
                // 获取笔记关联的小部件类型
                mWidgetType = cursor.getInt(NOTE_WIDGET_TYPE_COLUMN);
                // 获取笔记的提醒日期
                mAlertDate = cursor.getLong(NOTE_ALERTED_DATE_COLUMN);
                // 获取笔记的修改日期
                mModifiedDate = cursor.getLong(NOTE_MODIFIED_DATE_COLUMN);
            }
            // 关闭游标，释放资源
            cursor.close();
        } else {
            // 若未找到指定 ID 的笔记，记录错误日志并抛出异常
            Log.e(TAG, "No note with id:" + mNoteId);
            throw new IllegalArgumentException("Unable to find note with id " + mNoteId);
        }
        // 加载笔记的数据
        loadNoteData();
    }

    /**
     * 加载笔记的数据，如文本内容、模式等
     */
    private void loadNoteData() {
        // 查询指定笔记 ID 的数据
        Cursor cursor = mContext.getContentResolver().query(Notes.CONTENT_DATA_URI, DATA_PROJECTION,
                DataColumns.NOTE_ID + "=?", new String[]{
                        String.valueOf(mNoteId)
                }, null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    // 获取数据的 MIME 类型
                    String type = cursor.getString(DATA_MIME_TYPE_COLUMN);
                    if (DataConstants.NOTE.equals(type)) {
                        // 若为文本笔记类型，获取笔记内容和模式，并设置文本数据 ID
                        mContent = cursor.getString(DATA_CONTENT_COLUMN);
                        mMode = cursor.getInt(DATA_MODE_COLUMN);
                        mNote.setTextDataId(cursor.getLong(DATA_ID_COLUMN));
                    } else if (DataConstants.CALL_NOTE.equals(type)) {
                        // 若为通话笔记类型，设置通话数据 ID
                        mNote.setCallDataId(cursor.getLong(DATA_ID_COLUMN));
                    } else {
                        // 若为其他未知类型，记录日志
                        Log.d(TAG, "Wrong note type with type:" + type);
                    }
                } while (cursor.moveToNext());
            }
            // 关闭游标，释放资源
            cursor.close();
        } else {
            // 若未找到指定笔记 ID 的数据，记录错误日志并抛出异常
            Log.e(TAG, "No data with id:" + mNoteId);
            throw new IllegalArgumentException("Unable to find note's data with id " + mNoteId);
        }
    }

    /**
     * 创建一个空的笔记对象
     * @param context 上下文对象
     * @param folderId 笔记所属文件夹的 ID
     * @param widgetId 笔记关联的小部件 ID
     * @param widgetType 笔记关联的小部件类型
     * @param defaultBgColorId 笔记的默认背景颜色 ID
     * @return 新创建的 WorkingNote 对象
     */
    public static WorkingNote createEmptyNote(Context context, long folderId, int widgetId,
                                              int widgetType, int defaultBgColorId) {
        WorkingNote note = new WorkingNote(context, folderId);
        // 设置笔记的背景颜色 ID
        note.setBgColorId(defaultBgColorId);
        // 设置笔记关联的小部件 ID
        note.setWidgetId(widgetId);
        // 设置笔记关联的小部件类型
        note.setWidgetType(widgetType);
        return note;
    }

    /**
     * 加载指定 ID 的笔记
     * @param context 上下文对象
     * @param id 笔记的 ID
     * @return 加载的 WorkingNote 对象
     */
    public static WorkingNote load(Context context, long id) {
        return new WorkingNote(context, id, 0);
    }

    /**
     * 保存笔记到数据库
     * @return 保存成功返回 true，失败返回 false
     */
    public synchronized boolean saveNote() {
        if (isWorthSaving()) {
            if (!existInDatabase()) {
                // 若笔记不存在于数据库中，创建新笔记
                if ((mNoteId = Note.getNewNoteId(mContext, mFolderId)) == 0) {
                    // 若创建新笔记失败，记录错误日志并返回 false
                    Log.e(TAG, "Create new note fail with id:" + mNoteId);
                    return false;
                }
            }
            // 同步笔记到数据库
            mNote.syncNote(mContext, mNoteId);

            /**
             * 若笔记关联了有效的小部件，且设置了监听器，通知小部件内容变化
             */
            if (mWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID
                    && mWidgetType != Notes.TYPE_WIDGET_INVALIDE
                    && mNoteSettingStatusListener != null) {
                mNoteSettingStatusListener.onWidgetChanged();
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * 判断笔记是否存在于数据库中
     * @return 存在返回 true，不存在返回 false
     */
    public boolean existInDatabase() {
        return mNoteId > 0;
    }

    /**
     * 判断笔记是否值得保存
     * @return 值得保存返回 true，否则返回 false
     */
    private boolean isWorthSaving() {
        if (mIsDeleted || (!existInDatabase() && TextUtils.isEmpty(mContent))
                || (existInDatabase() && !mNote.isLocalModified())) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * 设置笔记设置变化监听器
     * @param l 监听器对象
     */
    public void setOnSettingStatusChangedListener(NoteSettingChangedListener l) {
        mNoteSettingStatusListener = l;
    }

    /**
     * 设置笔记的提醒日期
     * @param date 提醒日期
     * @param set 是否设置提醒
     */
    public void setAlertDate(long date, boolean set) {
        if (date != mAlertDate) {
            mAlertDate = date;
            // 设置笔记的提醒日期属性
            mNote.setNoteValue(NoteColumns.ALERTED_DATE, String.valueOf(mAlertDate));
        }
        if (mNoteSettingStatusListener != null) {
            // 通知监听器提醒日期变化
            mNoteSettingStatusListener.onClockAlertChanged(date, set);
        }
    }

    /**
     * 标记笔记是否被删除
     * @param mark 是否标记为删除
     */
    public void markDeleted(boolean mark) {
        mIsDeleted = mark;
        if (mWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID
                && mWidgetType != Notes.TYPE_WIDGET_INVALIDE && mNoteSettingStatusListener != null) {
            // 若笔记关联了有效的小部件，且设置了监听器，通知小部件内容变化
            mNoteSettingStatusListener.onWidgetChanged();
        }
    }

    /**
     * 设置笔记的背景颜色 ID
     * @param id 背景颜色 ID
     */
    public void setBgColorId(int id) {
        if (id != mBgColorId) {
            mBgColorId = id;
            if (mNoteSettingStatusListener != null) {
                // 通知监听器背景颜色变化
                mNoteSettingStatusListener.onBackgroundColorChanged();
            }
            // 设置笔记的背景颜色属性
            mNote.setNoteValue(NoteColumns.BG_COLOR_ID, String.valueOf(id));
        }
    }

    /**
     * 设置笔记的清单模式
     * @param mode 清单模式
     */
    public void setCheckListMode(int mode) {
        if (mMode != mode) {
            if (mNoteSettingStatusListener != null) {
                // 通知监听器清单模式变化
                mNoteSettingStatusListener.onCheckListModeChanged(mMode, mode);
            }
            mMode = mode;
            // 设置笔记的清单模式属性
            mNote.setTextData(TextNote.MODE, String.valueOf(mMode));
        }
    }

    /**
     * 设置笔记关联的小部件类型
     * @param type 小部件类型
     */
    public void setWidgetType(int type) {
        if (type != mWidgetType) {
            mWidgetType = type;
            // 设置笔记的小部件类型属性
            mNote.setNoteValue(NoteColumns.WIDGET_TYPE, String.valueOf(mWidgetType));
        }
    }

    /**
     * 设置笔记关联的小部件 ID
     * @param id 小部件 ID
     */
    public void setWidgetId(int id) {
        if (id != mWidgetId) {
            mWidgetId = id;
            // 设置笔记的小部件 ID 属性
            mNote.setNoteValue(NoteColumns.WIDGET_ID, String.valueOf(mWidgetId));
        }
    }

    /**
     * 设置笔记的文本内容
     * @param text 文本内容
     */
    public void setWorkingText(String text) {
        if (!TextUtils.equals(mContent, text)) {
            mContent = text;
            // 设置笔记的文本内容属性
            mNote.setTextData(DataColumns.CONTENT, mContent);
        }
    }

    /**
     * 将笔记转换为通话笔记
     * @param phoneNumber 电话号码
     * @param callDate 通话日期
     */
    public void convertToCallNote(String phoneNumber, long callDate) {
        // 设置通话笔记的通话日期属性
        mNote.setCallData(CallNote.CALL_DATE, String.valueOf(callDate));
        // 设置通话笔记的电话号码属性
        mNote.setCallData(CallNote.PHONE_NUMBER, phoneNumber);
        // 设置笔记所属文件夹为通话记录文件夹
        mNote.setNoteValue(NoteColumns.PARENT_ID, String.valueOf(Notes.ID_CALL_RECORD_FOLDER));
    }

    /**
     * 判断笔记是否设置了提醒
     * @return 设置了提醒返回 true，否则返回 false
     */
    public boolean hasClockAlert() {
        return (mAlertDate > 0 ? true : false);
    }

    /**
     * 获取笔记的内容
     * @return 笔记的内容
     */
    public String getContent() {
        return mContent;
    }

    /**
     * 获取笔记的提醒日期
     * @return 笔记的提醒日期
     */
    public long getAlertDate() {
        return mAlertDate;
    }

    /**
     * 获取笔记的修改日期
     * @return 笔记的修改日期
     */
    public long getModifiedDate() {
        return mModifiedDate;
    }

    /**
     * 获取笔记背景颜色的资源 ID
     * @return 背景颜色的资源 ID
     */
    public int getBgColorResId() {
        return NoteBgResources.getNoteBgResource(mBgColorId);
    }

    /**
     * 获取笔记的背景颜色 ID
     * @return 背景颜色 ID
     */
    public int getBgColorId() {
        return mBgColorId;
    }

    /**
     * 获取笔记标题背景的资源 ID
     * @return 标题背景的资源 ID
     */
    public int getTitleBgResId() {
        return NoteBgResources.getNoteTitleBgResource(mBgColorId);
    }

    /**
     * 获取笔记的清单模式
     * @return 清单模式
     */
    public int getCheckListMode() {
        return mMode;
    }

    /**
     * 获取笔记的 ID
     * @return 笔记的 ID
     */
    public long getNoteId() {
        return mNoteId;
    }

    /**
     * 获取笔记所属文件夹的 ID
     * @return 文件夹的 ID
     */
    public long getFolderId() {
        return mFolderId;
    }

    /**
     * 获取笔记关联的小部件 ID
     * @return 小部件的 ID
     */
    public int getWidgetId() {
        return mWidgetId;
    }

    /**
     * 获取笔记关联的小部件类型
     * @return 小部件的类型
     */
    public int getWidgetType() {
        return mWidgetType;
    }

    // 笔记设置变化监听器接口，用于监听笔记设置的变化
    public interface NoteSettingChangedListener {
        /**
         * 当笔记的背景颜色发生变化时调用
         */
        void onBackgroundColorChanged();

        /**
         * 当用户设置提醒时钟时调用
         * @param date 提醒日期
         * @param set 是否设置提醒
         */
        void onClockAlertChanged(long date, boolean set);

        /**
         * 当用户通过小部件创建笔记时调用
         */
        void onWidgetChanged();

        /**
         * 当笔记在清单模式和普通模式之间切换时调用
         * @param oldMode 切换前的模式
         * @param newMode 切换后的模式
         */
        void onCheckListModeChanged(int oldMode, int newMode);
    }
}
