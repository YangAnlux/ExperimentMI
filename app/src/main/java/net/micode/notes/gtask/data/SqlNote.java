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

package net.micode.notes.gtask.data;

import android.appwidget.AppWidgetManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.DataColumns;
import net.micode.notes.data.Notes.NoteColumns;
import net.micode.notes.gtask.exception.ActionFailureException;
import net.micode.notes.tool.GTaskStringUtils;
import net.micode.notes.tool.ResourceParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/*
 * Description：用于支持小米便签最底层的数据库相关操作，和sqldata的关系上是父集关系，即note是data的子父集。
 * 和SqlData相比，SqlNote算是真正意义上的数据了。
 */


public class SqlNote {
    /*
     * 得到类的简写名称存入字符串TAG中
     * 调用getSimpleName ()函数     */
    private static final String TAG = SqlNote.class.getSimpleName();

    private static final int INVALID_ID = -99999;

    // 集合了interface NoteColumns中所有SF常量（17个）
    public static final String[] PROJECTION_NOTE = new String[]{
            NoteColumns.ID, NoteColumns.ALERTED_DATE, NoteColumns.BG_COLOR_ID,
            NoteColumns.CREATED_DATE, NoteColumns.HAS_ATTACHMENT, NoteColumns.MODIFIED_DATE,
            NoteColumns.NOTES_COUNT, NoteColumns.PARENT_ID, NoteColumns.SNIPPET, NoteColumns.TYPE,
            NoteColumns.WIDGET_ID, NoteColumns.WIDGET_TYPE, NoteColumns.SYNC_ID,
            NoteColumns.LOCAL_MODIFIED, NoteColumns.ORIGIN_PARENT_ID, NoteColumns.GTASK_ID,
            NoteColumns.VERSION
    };

    //以下设置17个列的编号
    public static final int ID_COLUMN = 0;

    public static final int ALERTED_DATE_COLUMN = 1;

    public static final int BG_COLOR_ID_COLUMN = 2;

    public static final int CREATED_DATE_COLUMN = 3;

    public static final int HAS_ATTACHMENT_COLUMN = 4;

    public static final int MODIFIED_DATE_COLUMN = 5;

    public static final int NOTES_COUNT_COLUMN = 6;

    public static final int PARENT_ID_COLUMN = 7;

    public static final int SNIPPET_COLUMN = 8;

    public static final int TYPE_COLUMN = 9;

    public static final int WIDGET_ID_COLUMN = 10;

    public static final int WIDGET_TYPE_COLUMN = 11;

    public static final int SYNC_ID_COLUMN = 12;

    public static final int LOCAL_MODIFIED_COLUMN = 13;

    public static final int ORIGIN_PARENT_ID_COLUMN = 14;

    public static final int GTASK_ID_COLUMN = 15;

    public static final int VERSION_COLUMN = 16;

    //一下定义了17个内部的变量，其中12个可以由content中获得，5个需要初始化为0或者new
    private Context mContext;

    private ContentResolver mContentResolver;

    private boolean mIsCreate;

    private long mId;

    private long mAlertDate;

    private int mBgColorId;

    private long mCreatedDate;

    private int mHasAttachment;

    private long mModifiedDate;

    private long mParentId;

    private String mSnippet;

    private int mType;

    private int mWidgetId;

    private int mWidgetType;

    private long mOriginParent;

    private long mVersion;

    private ContentValues mDiffNoteValues;

    private ArrayList<SqlData> mDataList;

    /*
     * 构造函数
     *  mIsCreate用于标示构造方式
     */
    //构造函数只有context，对所有的变量进行初始化
    public SqlNote(Context context) {
        mContext = context;
        mContentResolver = context.getContentResolver();
        mIsCreate = true;
        mId = INVALID_ID;
        mAlertDate = 0;
        mBgColorId = ResourceParser.getDefaultBgId(context);
        mCreatedDate = System.currentTimeMillis();//调用系统函数获得创建时间
        mHasAttachment = 0;
        mModifiedDate = System.currentTimeMillis();//最后一次修改时间初始化为创建时间
        mParentId = 0;
        mSnippet = "";
        mType = Notes.TYPE_NOTE;
        mWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
        mWidgetType = Notes.TYPE_WIDGET_INVALIDE;
        mOriginParent = 0;
        mVersion = 0;
        mDiffNoteValues = new ContentValues();
        mDataList = new ArrayList<SqlData>();
    }

    /*
     * 构造函数
     *  mIsCreate用于标示构造方式
     */
    //构造函数有context和一个数据库的cursor，多数变量通过cursor指向的一条记录直接进行初始化
    public SqlNote(Context context, Cursor c) {
        mContext = context;
        mContentResolver = context.getContentResolver();
        mIsCreate = false;
        loadFromCursor(c);
        mDataList = new ArrayList<SqlData>();
        if (mType == Notes.TYPE_NOTE)
            loadDataContent();
        mDiffNoteValues = new ContentValues();
    }
    /*
     * 构造函数
     *  mIsCreate用于标示构造方式
     */

    public SqlNote(Context context, long id) {
        mContext = context;
        mContentResolver = context.getContentResolver();
        mIsCreate = false;
        loadFromCursor(id);
        mDataList = new ArrayList<SqlData>();
        if (mType == Notes.TYPE_NOTE)
            loadDataContent();
        mDiffNoteValues = new ContentValues();

    }

    /*
     * 通过id从光标处加载数据
     */
    private void loadFromCursor(long id) {
        Cursor c = null;
        try {
            c = mContentResolver.query(Notes.CONTENT_NOTE_URI, PROJECTION_NOTE, "(_id=?)",
                    new String[]{
                            String.valueOf(id)
                    }, null);//通过id获得对应的ContentResolver中的cursor
            if (c != null) {
                c.moveToNext();
                loadFromCursor(c);//然后加载数据进行初始化

            } else {
                Log.w(TAG, "loadFromCursor: cursor = null");
            }
        } finally {
            if (c != null)
                c.close();
        }
    }

    /*
     * 通过游标从光标处加载数据
     */
    private void loadFromCursor(Cursor c) {

        //直接从一条记录中的获得以下变量的初始值
        mId = c.getLong(ID_COLUMN);
        mAlertDate = c.getLong(ALERTED_DATE_COLUMN);
        mBgColorId = c.getInt(BG_COLOR_ID_COLUMN);
        mCreatedDate = c.getLong(CREATED_DATE_COLUMN);
        mHasAttachment = c.getInt(HAS_ATTACHMENT_COLUMN);
        mModifiedDate = c.getLong(MODIFIED_DATE_COLUMN);
        mParentId = c.getLong(PARENT_ID_COLUMN);
        mSnippet = c.getString(SNIPPET_COLUMN);
        mType = c.getInt(TYPE_COLUMN);
        mWidgetId = c.getInt(WIDGET_ID_COLUMN);
        mWidgetType = c.getInt(WIDGET_TYPE_COLUMN);
        mVersion = c.getLong(VERSION_COLUMN);
    }

    /*
     * 通过content机制获取共享数据并加载到数据库当前游标处     */
    private void loadDataContent() {
        Cursor c = null;
        mDataList.clear();
        try {
            c = mContentResolver.query(Notes.CONTENT_DATA_URI, SqlData.PROJECTION_DATA,
                    "(note_id=?)", new String[]{
                            String.valueOf(mId)
                    }, null);
            if (c != null) {
                if (c.getCount() == 0) {
                    Log.w(TAG, "it seems that the note has not data");
                    return;
                }
                while (c.moveToNext()) {
                    SqlData data = new SqlData(mContext, c);
                    mDataList.add(data);
                }
            } else {
                Log.w(TAG, "loadDataContent: cursor = null");
            }
        } finally {
            if (c != null)
                c.close();
        }
    }

    /*
     * 设置通过content机制用于共享的数据信息     */
    public boolean setContent(JSONObject js) {
        try {
            JSONObject note = js.getJSONObject(GTaskStringUtils.META_HEAD_NOTE);
            if (note.getInt(NoteColumns.TYPE) == Notes.TYPE_SYSTEM) {
                Log.w(TAG, "cannot set system folder");
            } else if (note.getInt(NoteColumns.TYPE) == Notes.TYPE_FOLDER) {
                // for folder we can only update the snnipet and type
                String snippet = note.has(NoteColumns.SNIPPET) ? note
                        .getString(NoteColumns.SNIPPET) : "";
                if (mIsCreate || !mSnippet.equals(snippet)) {
                    mDiffNoteValues.put(NoteColumns.SNIPPET, snippet);
                }
                mSnippet = snippet;

                int type = note.has(NoteColumns.TYPE) ? note.getInt(NoteColumns.TYPE)
                        : Notes.TYPE_NOTE;
                if (mIsCreate || mType != type) {
                    mDiffNoteValues.put(NoteColumns.TYPE, type);
                }
                mType = type;
            } else if (note.getInt(NoteColumns.TYPE) == Notes.TYPE_NOTE) {
                JSONArray dataArray = js.getJSONArray(GTaskStringUtils.META_HEAD_DATA);
                long id = note.has(NoteColumns.ID) ? note.getLong(NoteColumns.ID) : INVALID_ID;
                if (mIsCreate || mId != id) {
                    mDiffNoteValues.put(NoteColumns.ID, id);
                }
                mId = id;

                long alertDate = note.has(NoteColumns.ALERTED_DATE) ? note
                        .getLong(NoteColumns.ALERTED_DATE) : 0;
                if (mIsCreate || mAlertDate != alertDate) {
                    mDiffNoteValues.put(NoteColumns.ALERTED_DATE, alertDate);
                }
                mAlertDate = alertDate;

                int bgColorId = note.has(NoteColumns.BG_COLOR_ID) ? note
                        .getInt(NoteColumns.BG_COLOR_ID) : ResourceParser.getDefaultBgId(mContext);
                if (mIsCreate || mBgColorId != bgColorId) {
                    mDiffNoteValues.put(NoteColumns.BG_COLOR_ID, bgColorId);
                }
                mBgColorId = bgColorId;

                long createDate = note.has(NoteColumns.CREATED_DATE) ? note
                        .getLong(NoteColumns.CREATED_DATE) : System.currentTimeMillis();
                if (mIsCreate || mCreatedDate != createDate) {
                    mDiffNoteValues.put(NoteColumns.CREATED_DATE, createDate);
                }
                mCreatedDate = createDate;

                int hasAttachment = note.has(NoteColumns.HAS_ATTACHMENT) ? note
                        .getInt(NoteColumns.HAS_ATTACHMENT) : 0;
                if (mIsCreate || mHasAttachment != hasAttachment) {
                    mDiffNoteValues.put(NoteColumns.HAS_ATTACHMENT, hasAttachment);
                }
                mHasAttachment = hasAttachment;

                long modifiedDate = note.has(NoteColumns.MODIFIED_DATE) ? note
                        .getLong(NoteColumns.MODIFIED_DATE) : System.currentTimeMillis();
                if (mIsCreate || mModifiedDate != modifiedDate) {
                    mDiffNoteValues.put(NoteColumns.MODIFIED_DATE, modifiedDate);
                }
                mModifiedDate = modifiedDate;

                long parentId = note.has(NoteColumns.PARENT_ID) ? note
                        .getLong(NoteColumns.PARENT_ID) : 0;
                if (mIsCreate || mParentId != parentId) {
                    mDiffNoteValues.put(NoteColumns.PARENT_ID, parentId);
                }
                mParentId = parentId;

                String snippet = note.has(NoteColumns.SNIPPET) ? note
                        .getString(NoteColumns.SNIPPET) : "";
                if (mIsCreate || !mSnippet.equals(snippet)) {
                    mDiffNoteValues.put(NoteColumns.SNIPPET, snippet);
                }
                mSnippet = snippet;

                int type = note.has(NoteColumns.TYPE) ? note.getInt(NoteColumns.TYPE)
                        : Notes.TYPE_NOTE;
                if (mIsCreate || mType != type) {
                    mDiffNoteValues.put(NoteColumns.TYPE, type);
                }
                mType = type;

                int widgetId = note.has(NoteColumns.WIDGET_ID) ? note.getInt(NoteColumns.WIDGET_ID)
                        : AppWidgetManager.INVALID_APPWIDGET_ID;
                if (mIsCreate || mWidgetId != widgetId) {
                    mDiffNoteValues.put(NoteColumns.WIDGET_ID, widgetId);
                }
                mWidgetId = widgetId;

                int widgetType = note.has(NoteColumns.WIDGET_TYPE) ? note
                        .getInt(NoteColumns.WIDGET_TYPE) : Notes.TYPE_WIDGET_INVALIDE;
                if (mIsCreate || mWidgetType != widgetType) {
                    mDiffNoteValues.put(NoteColumns.WIDGET_TYPE, widgetType);
                }
                mWidgetType = widgetType;

                long originParent = note.has(NoteColumns.ORIGIN_PARENT_ID) ? note
                        .getLong(NoteColumns.ORIGIN_PARENT_ID) : 0;
                if (mIsCreate || mOriginParent != originParent) {
                    mDiffNoteValues.put(NoteColumns.ORIGIN_PARENT_ID, originParent);
                }
                mOriginParent = originParent;

                for (int i = 0; i < dataArray.length(); i++) {
                    JSONObject data = dataArray.getJSONObject(i);
                    SqlData sqlData = null;
                    if (data.has(DataColumns.ID)) {
                        long dataId = data.getLong(DataColumns.ID);
                        for (SqlData temp : mDataList) {
                            if (dataId == temp.getId()) {
                                sqlData = temp;
                            }
                        }
                    }

                    if (sqlData == null) {
                        sqlData = new SqlData(mContext);
                        mDataList.add(sqlData);
                    }

                    sqlData.setContent(data);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /*
     * 获取content机制提供的数据并加载到note中     */
    public JSONObject getContent() {
        try {
            JSONObject js = new JSONObject();

            if (mIsCreate) {
                Log.e(TAG, "it seems that we haven't created this in database yet");
                return null;
            }

            JSONObject note = new JSONObject();
            //类型为note时
            if (mType == Notes.TYPE_NOTE) {
                note.put(NoteColumns.ID, mId);
                note.put(NoteColumns.ALERTED_DATE, mAlertDate);
                note.put(NoteColumns.BG_COLOR_ID, mBgColorId);
                note.put(NoteColumns.CREATED_DATE, mCreatedDate);
                note.put(NoteColumns.HAS_ATTACHMENT, mHasAttachment);
                note.put(NoteColumns.MODIFIED_DATE, mModifiedDate);
                note.put(NoteColumns.PARENT_ID, mParentId);
                note.put(NoteColumns.SNIPPET, mSnippet);
                note.put(NoteColumns.TYPE, mType);
                note.put(NoteColumns.WIDGET_ID, mWidgetId);
                note.put(NoteColumns.WIDGET_TYPE, mWidgetType);
                note.put(NoteColumns.ORIGIN_PARENT_ID, mOriginParent);
                js.put(GTaskStringUtils.META_HEAD_NOTE, note);

                JSONArray dataArray = new JSONArray();
                for (SqlData sqlData : mDataList) {
                    JSONObject data = sqlData.getContent();
                    if (data != null) {
                        dataArray.put(data);
                    }
                }
                js.put(GTaskStringUtils.META_HEAD_DATA, dataArray);
                //类型为文件夹
            } else if (mType == Notes.TYPE_FOLDER || mType == Notes.TYPE_SYSTEM) {
                note.put(NoteColumns.ID, mId);
                note.put(NoteColumns.TYPE, mType);
                note.put(NoteColumns.SNIPPET, mSnippet);
                js.put(GTaskStringUtils.META_HEAD_NOTE, note);
            }

            return js;
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }
        return null;
    }

    /*
     * 给当前id设置父id
     */
    public void setParentId(long id) {
        mParentId = id;
        mDiffNoteValues.put(NoteColumns.PARENT_ID, id);
    }

    /*
     * 给当前id设置Gtaskid
     */
    public void setGtaskId(String gid) {
        mDiffNoteValues.put(NoteColumns.GTASK_ID, gid);
    }

    /*
     * 给当前id设置同步id
     */
    public void setSyncId(long syncId) {
        mDiffNoteValues.put(NoteColumns.SYNC_ID, syncId);
    }

    /*
     * 初始化本地修改，即撤销所有当前修改
     */
    public void resetLocalModified() {
        mDiffNoteValues.put(NoteColumns.LOCAL_MODIFIED, 0);
    }

    /*
     * 获得当前id
     */
    public long getId() {
        return mId;
    }

    /*
     * 获得当前id的父id
     */
    public long getParentId() {
        return mParentId;
    }

    /*
     * 获取小片段即用于显示的部分便签内容
     */
    public String getSnippet() {
        return mSnippet;
    }

    /*
     * 判断是否为便签类型
     */
    public boolean isNoteType() {
        return mType == Notes.TYPE_NOTE;
    }

    /*
     * commit函数用于把当前造作所做的修改保存到数据库
     */
    public void commit(boolean validateVersion) {
        if (mIsCreate) {
            if (mId == INVALID_ID && mDiffNoteValues.containsKey(NoteColumns.ID)) {
                mDiffNoteValues.remove(NoteColumns.ID);
            }

            Uri uri = mContentResolver.insert(Notes.CONTENT_NOTE_URI, mDiffNoteValues);
            try {
                mId = Long.valueOf(uri.getPathSegments().get(1));
            } catch (NumberFormatException e) {
                Log.e(TAG, "Get note id error :" + e.toString());
                throw new ActionFailureException("create note failed");
            }
            if (mId == 0) {
                throw new IllegalStateException("Create thread id failed");
            }

            if (mType == Notes.TYPE_NOTE) {
                //直接使用sqldata中的实现
                for (SqlData sqlData : mDataList) {
                    sqlData.commit(mId, false, -1);
                }
            }
        } else {
            if (mId <= 0 && mId != Notes.ID_ROOT_FOLDER && mId != Notes.ID_CALL_RECORD_FOLDER) {
                Log.e(TAG, "No such note");
                throw new IllegalStateException("Try to update note with invalid id");
            }
            if (mDiffNoteValues.size() > 0) {
                mVersion++;
                int result = 0;
                //构造字符串
                if (!validateVersion) {
                    result = mContentResolver.update(Notes.CONTENT_NOTE_URI, mDiffNoteValues, "("
                            + NoteColumns.ID + "=?)", new String[]{
                            String.valueOf(mId)
                    });
                } else {
                    result = mContentResolver.update(Notes.CONTENT_NOTE_URI, mDiffNoteValues, "("
                                    + NoteColumns.ID + "=?) AND (" + NoteColumns.VERSION + "<=?)",
                            new String[]{
                                    String.valueOf(mId), String.valueOf(mVersion)
                            });
                }
                if (result == 0) {
                    Log.w(TAG, "there is no update. maybe user updates note when syncing");
                }
            }

            if (mType == Notes.TYPE_NOTE) {
                for (SqlData sqlData : mDataList) {
                    sqlData.commit(mId, validateVersion, mVersion);
                }
            }
        }

        // refresh local info
        loadFromCursor(mId);
        if (mType == Notes.TYPE_NOTE)
            loadDataContent();

        mDiffNoteValues.clear();
        mIsCreate = false;
    }
}
