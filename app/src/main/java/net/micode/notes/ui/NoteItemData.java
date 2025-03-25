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

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

import net.micode.notes.data.Contact;
import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.NoteColumns;
import net.micode.notes.tool.DataUtils;

// 用于存储笔记项数据的类
public class NoteItemData {
    // 定义查询投影，指定从数据库中获取的列
    static final String[] PROJECTION = new String[]{
            NoteColumns.ID,
            NoteColumns.ALERTED_DATE,
            NoteColumns.BG_COLOR_ID,
            NoteColumns.CREATED_DATE,
            NoteColumns.HAS_ATTACHMENT,
            NoteColumns.MODIFIED_DATE,
            NoteColumns.NOTES_COUNT,
            NoteColumns.PARENT_ID,
            NoteColumns.SNIPPET,
            NoteColumns.TYPE,
            NoteColumns.WIDGET_ID,
            NoteColumns.WIDGET_TYPE,
    };

    // 定义投影中各列的索引，方便后续使用
    private static final int ID_COLUMN = 0;
    private static final int ALERTED_DATE_COLUMN = 1;
    private static final int BG_COLOR_ID_COLUMN = 2;
    private static final int CREATED_DATE_COLUMN = 3;
    private static final int HAS_ATTACHMENT_COLUMN = 4;
    private static final int MODIFIED_DATE_COLUMN = 5;
    private static final int NOTES_COUNT_COLUMN = 6;
    private static final int PARENT_ID_COLUMN = 7;
    private static final int SNIPPET_COLUMN = 8;
    private static final int TYPE_COLUMN = 9;
    private static final int WIDGET_ID_COLUMN = 10;
    private static final int WIDGET_TYPE_COLUMN = 11;

    // 存储笔记项的各种属性
    private long mId; // 笔记项的ID
    private long mAlertDate; // 提醒日期
    private int mBgColorId; // 背景颜色ID
    private long mCreatedDate; // 创建日期
    private boolean mHasAttachment; // 是否有附件
    private long mModifiedDate; // 修改日期
    private int mNotesCount; // 笔记数量（可能用于文件夹类型的笔记）
    private long mParentId; // 父ID（用于表示所属文件夹）
    private String mSnippet; // 笔记摘要
    private int mType; // 笔记类型
    private int mWidgetId; // 小部件ID
    private int mWidgetType; // 小部件类型
    private String mName; // 联系人名称（如果是通话记录相关的笔记）
    private String mPhoneNumber; // 电话号码（如果是通话记录相关的笔记）

    // 用于表示笔记项在列表中的位置属性
    private boolean mIsLastItem; // 是否是最后一项
    private boolean mIsFirstItem; // 是否是第一项
    private boolean mIsOnlyOneItem; // 是否是唯一一项
    private boolean mIsOneNoteFollowingFolder; // 是否是文件夹后跟随的单个笔记
    private boolean mIsMultiNotesFollowingFolder; // 是否是文件夹后跟随的多个笔记

    // 构造函数，根据上下文和数据库游标初始化笔记项数据
    public NoteItemData(Context context, Cursor cursor) {
        // 从游标中获取各项数据并赋值给对应的成员变量
        mId = cursor.getLong(ID_COLUMN);
        mAlertDate = cursor.getLong(ALERTED_DATE_COLUMN);
        mBgColorId = cursor.getInt(BG_COLOR_ID_COLUMN);
        mCreatedDate = cursor.getLong(CREATED_DATE_COLUMN);
        mHasAttachment = (cursor.getInt(HAS_ATTACHMENT_COLUMN) > 0) ? true : false;
        mModifiedDate = cursor.getLong(MODIFIED_DATE_COLUMN);
        mNotesCount = cursor.getInt(NOTES_COUNT_COLUMN);
        mParentId = cursor.getLong(PARENT_ID_COLUMN);
        mSnippet = cursor.getString(SNIPPET_COLUMN);
        // 去除笔记摘要中的已选中和未选中标签
        mSnippet = mSnippet.replace(NoteEditActivity.TAG_CHECKED, "").replace(
                NoteEditActivity.TAG_UNCHECKED, "");
        mType = cursor.getInt(TYPE_COLUMN);
        mWidgetId = cursor.getInt(WIDGET_ID_COLUMN);
        mWidgetType = cursor.getInt(WIDGET_TYPE_COLUMN);

        mPhoneNumber = "";
        // 如果父ID是通话记录文件夹的ID，则获取电话号码和联系人名称
        if (mParentId == Notes.ID_CALL_RECORD_FOLDER) {
            mPhoneNumber = DataUtils.getCallNumberByNoteId(context.getContentResolver(), mId);
            if (!TextUtils.isEmpty(mPhoneNumber)) {
                mName = Contact.getContact(context, mPhoneNumber);
                if (mName == null) {
                    mName = mPhoneNumber;
                }
            }
        }

        if (mName == null) {
            mName = "";
        }
        // 检查笔记项在列表中的位置属性
        checkPostion(cursor);
    }

    // 检查笔记项在列表中的位置属性的方法
    private void checkPostion(Cursor cursor) {
        // 设置是否是最后一项、第一项、唯一一项的属性
        mIsLastItem = cursor.isLast() ? true : false;
        mIsFirstItem = cursor.isFirst() ? true : false;
        mIsOnlyOneItem = (cursor.getCount() == 1);
        mIsMultiNotesFollowingFolder = false;
        mIsOneNoteFollowingFolder = false;

        // 如果是笔记类型且不是第一项，则检查是否是文件夹后跟随的笔记
        if (mType == Notes.TYPE_NOTE && !mIsFirstItem) {
            int position = cursor.getPosition();
            if (cursor.moveToPrevious()) {
                // 如果前一项是文件夹或系统类型
                if (cursor.getInt(TYPE_COLUMN) == Notes.TYPE_FOLDER
                        || cursor.getInt(TYPE_COLUMN) == Notes.TYPE_SYSTEM) {
                    if (cursor.getCount() > (position + 1)) {
                        mIsMultiNotesFollowingFolder = true;
                    } else {
                        mIsOneNoteFollowingFolder = true;
                    }
                }
                // 将游标移回原来的位置
                if (!cursor.moveToNext()) {
                    throw new IllegalStateException("cursor move to previous but can't move back");
                }
            }
        }
    }

    // 判断是否是文件夹后跟随的单个笔记
    public boolean isOneFollowingFolder() {
        return mIsOneNoteFollowingFolder;
    }

    // 判断是否是文件夹后跟随的多个笔记
    public boolean isMultiFollowingFolder() {
        return mIsMultiNotesFollowingFolder;
    }

    // 判断是否是最后一项
    public boolean isLast() {
        return mIsLastItem;
    }

    // 获取联系人名称（如果是通话记录相关的笔记）
    public String getCallName() {
        return mName;
    }

    // 判断是否是第一项
    public boolean isFirst() {
        return mIsFirstItem;
    }

    // 判断是否是唯一一项
    public boolean isSingle() {
        return mIsOnlyOneItem;
    }

    // 获取笔记项的ID
    public long getId() {
        return mId;
    }

    // 获取提醒日期
    public long getAlertDate() {
        return mAlertDate;
    }

    // 获取创建日期
    public long getCreatedDate() {
        return mCreatedDate;
    }

    // 判断是否有附件
    public boolean hasAttachment() {
        return mHasAttachment;
    }

    // 获取修改日期
    public long getModifiedDate() {
        return mModifiedDate;
    }

    // 获取背景颜色ID
    public int getBgColorId() {
        return mBgColorId;
    }

    // 获取父ID
    public long getParentId() {
        return mParentId;
    }

    // 获取笔记数量（可能用于文件夹类型的笔记）
    public int getNotesCount() {
        return mNotesCount;
    }

    // 获取文件夹ID（等同于父ID）
    public long getFolderId() {
        return mParentId;
    }

    // 获取笔记类型
    public int getType() {
        return mType;
    }

    // 获取小部件类型
    public int getWidgetType() {
        return mWidgetType;
    }

    // 获取小部件ID
    public int getWidgetId() {
        return mWidgetId;
    }

    // 获取笔记摘要
    public String getSnippet() {
        return mSnippet;
    }

    // 判断是否有提醒
    public boolean hasAlert() {
        return (mAlertDate > 0);
    }

    // 判断是否是通话记录相关的笔记
    public boolean isCallRecord() {
        return (mParentId == Notes.ID_CALL_RECORD_FOLDER && !TextUtils.isEmpty(mPhoneNumber));
    }

    // 从游标中获取笔记类型的静态方法
    public static int getNoteType(Cursor cursor) {
        return cursor.getInt(TYPE_COLUMN);
    }
}
