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

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;

import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.CallNote;
import net.micode.notes.data.Notes.DataColumns;
import net.micode.notes.data.Notes.NoteColumns;
import net.micode.notes.data.Notes.TextNote;

import java.util.ArrayList;

// Note 类用于管理笔记及其相关数据的创建、修改和同步操作
public class Note {
    // 用于存储笔记的差异值，即有改动的笔记信息
    private ContentValues mNoteDiffValues;
    // 存储笔记的文本和通话数据
    private NoteData mNoteData;
    // 日志标签，用于在日志中标识该类的相关信息
    private static final String TAG = "Note";

    /**
     * 创建一个新的笔记 ID 并将新笔记添加到数据库中
     * @param context 上下文对象
     * @param folderId 笔记所属文件夹的 ID
     * @return 新笔记的 ID
     */
    public static synchronized long getNewNoteId(Context context, long folderId) {
        // 创建新笔记时需要插入的内容值
        ContentValues values = new ContentValues();
        // 获取当前时间作为创建时间和修改时间
        long createdTime = System.currentTimeMillis();
        values.put(NoteColumns.CREATED_DATE, createdTime);
        values.put(NoteColumns.MODIFIED_DATE, createdTime);
        // 设置笔记类型为普通笔记
        values.put(NoteColumns.TYPE, Notes.TYPE_NOTE);
        // 标记笔记为本地修改
        values.put(NoteColumns.LOCAL_MODIFIED, 1);
        // 设置笔记所属文件夹的 ID
        values.put(NoteColumns.PARENT_ID, folderId);

        // 插入新笔记到数据库，并获取返回的 URI
        Uri uri = context.getContentResolver().insert(Notes.CONTENT_NOTE_URI, values);

        long noteId = 0;
        try {
            // 从 URI 中提取新笔记的 ID
            noteId = Long.valueOf(uri.getPathSegments().get(1));
        } catch (NumberFormatException e) {
            // 若提取 ID 失败，记录错误日志
            Log.e(TAG, "Get note id error :" + e.toString());
            noteId = 0;
        }
        // 若笔记 ID 为 -1，抛出异常表示笔记 ID 错误
        if (noteId == -1) {
            throw new IllegalStateException("Wrong note id:" + noteId);
        }
        return noteId;
    }

    // 构造函数，初始化笔记差异值和笔记数据
    public Note() {
        mNoteDiffValues = new ContentValues();
        mNoteData = new NoteData();
    }

    /**
     * 设置笔记的值，并标记笔记为本地修改，更新修改时间
     * @param key 笔记属性的键
     * @param value 笔记属性的值
     */
    public void setNoteValue(String key, String value) {
        mNoteDiffValues.put(key, value);
        mNoteDiffValues.put(NoteColumns.LOCAL_MODIFIED, 1);
        mNoteDiffValues.put(NoteColumns.MODIFIED_DATE, System.currentTimeMillis());
    }

    /**
     * 设置笔记的文本数据
     * @param key 文本数据的键
     * @param value 文本数据的值
     */
    public void setTextData(String key, String value) {
        mNoteData.setTextData(key, value);
    }

    /**
     * 设置笔记文本数据的 ID
     * @param id 文本数据的 ID
     */
    public void setTextDataId(long id) {
        mNoteData.setTextDataId(id);
    }

    /**
     * 获取笔记文本数据的 ID
     * @return 文本数据的 ID
     */
    public long getTextDataId() {
        return mNoteData.mTextDataId;
    }

    /**
     * 设置笔记通话数据的 ID
     * @param id 通话数据的 ID
     */
    public void setCallDataId(long id) {
        mNoteData.setCallDataId(id);
    }

    /**
     * 设置笔记的通话数据
     * @param key 通话数据的键
     * @param value 通话数据的值
     */
    public void setCallData(String key, String value) {
        mNoteData.setCallData(key, value);
    }

    /**
     * 判断笔记是否有本地修改
     * @return 若有修改返回 true，否则返回 false
     */
    public boolean isLocalModified() {
        return mNoteDiffValues.size() > 0 || mNoteData.isLocalModified();
    }

    /**
     * 同步笔记到数据库
     * @param context 上下文对象
     * @param noteId 笔记的 ID
     * @return 同步成功返回 true，失败返回 false
     */
    public boolean syncNote(Context context, long noteId) {
        // 检查笔记 ID 是否合法
        if (noteId <= 0) {
            throw new IllegalArgumentException("Wrong note id:" + noteId);
        }
        // 若笔记没有本地修改，直接返回同步成功
        if (!isLocalModified()) {
            return true;
        }

        /**
         * 理论上，一旦数据发生变化，笔记的 LOCAL_MODIFIED 和 MODIFIED_DATE 应该更新。
         * 为了数据安全，即使更新笔记失败，我们也会更新笔记数据信息
         */
        if (context.getContentResolver().update(
                ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, noteId), mNoteDiffValues, null,
                null) == 0) {
            // 若更新笔记失败，记录错误日志
            Log.e(TAG, "Update note error, should not happen");
            // 不返回，继续执行后续操作
        }
        // 清空笔记差异值
        mNoteDiffValues.clear();

        // 若笔记数据有本地修改，将其推送到 ContentResolver 并检查是否成功
        if (mNoteData.isLocalModified()
                && (mNoteData.pushIntoContentResolver(context, noteId) == null)) {
            return false;
        }

        return true;
    }

    // 内部类，用于管理笔记的文本和通话数据
    private class NoteData {
        // 文本数据的 ID
        private long mTextDataId;
        // 存储文本数据的内容值
        private ContentValues mTextDataValues;
        // 通话数据的 ID
        private long mCallDataId;
        // 存储通话数据的内容值
        private ContentValues mCallDataValues;
        // 日志标签，用于在日志中标识该内部类的相关信息
        private static final String TAG = "NoteData";

        // 构造函数，初始化文本和通话数据的内容值及 ID
        public NoteData() {
            mTextDataValues = new ContentValues();
            mCallDataValues = new ContentValues();
            mTextDataId = 0;
            mCallDataId = 0;
        }

        /**
         * 判断笔记数据是否有本地修改
         * @return 若有修改返回 true，否则返回 false
         */
        boolean isLocalModified() {
            return mTextDataValues.size() > 0 || mCallDataValues.size() > 0;
        }

        /**
         * 设置笔记文本数据的 ID
         * @param id 文本数据的 ID
         */
        void setTextDataId(long id) {
            // 检查文本数据 ID 是否合法
            if(id <= 0) {
                throw new IllegalArgumentException("Text data id should larger than 0");
            }
            mTextDataId = id;
        }

        /**
         * 设置笔记通话数据的 ID
         * @param id 通话数据的 ID
         */
        void setCallDataId(long id) {
            // 检查通话数据 ID 是否合法
            if (id <= 0) {
                throw new IllegalArgumentException("Call data id should larger than 0");
            }
            mCallDataId = id;
        }

        /**
         * 设置笔记的通话数据，并标记笔记为本地修改，更新修改时间
         * @param key 通话数据的键
         * @param value 通话数据的值
         */
        void setCallData(String key, String value) {
            mCallDataValues.put(key, value);
            mNoteDiffValues.put(NoteColumns.LOCAL_MODIFIED, 1);
            mNoteDiffValues.put(NoteColumns.MODIFIED_DATE, System.currentTimeMillis());
        }

        /**
         * 设置笔记的文本数据，并标记笔记为本地修改，更新修改时间
         * @param key 文本数据的键
         * @param value 文本数据的值
         */
        void setTextData(String key, String value) {
            mTextDataValues.put(key, value);
            mNoteDiffValues.put(NoteColumns.LOCAL_MODIFIED, 1);
            mNoteDiffValues.put(NoteColumns.MODIFIED_DATE, System.currentTimeMillis());
        }

        /**
         * 将笔记数据推送到 ContentResolver 进行同步
         * @param context 上下文对象
         * @param noteId 笔记的 ID
         * @return 同步成功返回笔记的 URI，失败返回 null
         */
        Uri pushIntoContentResolver(Context context, long noteId) {
            // 检查笔记 ID 是否合法
            if (noteId <= 0) {
                throw new IllegalArgumentException("Wrong note id:" + noteId);
            }

            // 创建一个操作列表，用于批量执行 ContentProvider 操作
            ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();
            ContentProviderOperation.Builder builder = null;

            // 若文本数据有修改
            if(mTextDataValues.size() > 0) {
                // 设置文本数据所属的笔记 ID
                mTextDataValues.put(DataColumns.NOTE_ID, noteId);
                // 若文本数据 ID 为 0，说明是新的文本数据
                if (mTextDataId == 0) {
                    // 设置文本数据的 MIME 类型
                    mTextDataValues.put(DataColumns.MIME_TYPE, TextNote.CONTENT_ITEM_TYPE);
                    // 插入新的文本数据到数据库，并获取返回的 URI
                    Uri uri = context.getContentResolver().insert(Notes.CONTENT_DATA_URI,
                            mTextDataValues);
                    try {
                        // 从 URI 中提取新文本数据的 ID
                        setTextDataId(Long.valueOf(uri.getPathSegments().get(1)));
                    } catch (NumberFormatException e) {
                        // 若提取 ID 失败，记录错误日志并清空文本数据
                        Log.e(TAG, "Insert new text data fail with noteId" + noteId);
                        mTextDataValues.clear();
                        return null;
                    }
                } else {
                    // 若文本数据 ID 不为 0，说明是更新已有文本数据
                    builder = ContentProviderOperation.newUpdate(ContentUris.withAppendedId(
                            Notes.CONTENT_DATA_URI, mTextDataId));
                    builder.withValues(mTextDataValues);
                    // 将更新操作添加到操作列表中
                    operationList.add(builder.build());
                }
                // 清空文本数据
                mTextDataValues.clear();
            }

            // 若通话数据有修改
            if(mCallDataValues.size() > 0) {
                // 设置通话数据所属的笔记 ID
                mCallDataValues.put(DataColumns.NOTE_ID, noteId);
                // 若通话数据 ID 为 0，说明是新的通话数据
                if (mCallDataId == 0) {
                    // 设置通话数据的 MIME 类型
                    mCallDataValues.put(DataColumns.MIME_TYPE, CallNote.CONTENT_ITEM_TYPE);
                    // 插入新的通话数据到数据库，并获取返回的 URI
                    Uri uri = context.getContentResolver().insert(Notes.CONTENT_DATA_URI,
                            mCallDataValues);
                    try {
                        // 从 URI 中提取新通话数据的 ID
                        setCallDataId(Long.valueOf(uri.getPathSegments().get(1)));
                    } catch (NumberFormatException e) {
                        // 若提取 ID 失败，记录错误日志并清空通话数据
                        Log.e(TAG, "Insert new call data fail with noteId" + noteId);
                        mCallDataValues.clear();
                        return null;
                    }
                } else {
                    // 若通话数据 ID 不为 0，说明是更新已有通话数据
                    builder = ContentProviderOperation.newUpdate(ContentUris.withAppendedId(
                            Notes.CONTENT_DATA_URI, mCallDataId));
                    builder.withValues(mCallDataValues);
                    // 将更新操作添加到操作列表中
                    operationList.add(builder.build());
                }
                // 清空通话数据
                mCallDataValues.clear();
            }

            // 若操作列表中有操作
            if (operationList.size() > 0) {
                try {
                    // 批量执行操作列表中的操作
                    ContentProviderResult[] results = context.getContentResolver().applyBatch(
                            Notes.AUTHORITY, operationList);
                    // 根据操作结果返回相应的 URI 或 null
                    return (results == null || results.length == 0 || results[0] == null) ? null
                            : ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, noteId);
                } catch (RemoteException e) {
                    // 若执行操作时发生远程异常，记录错误日志
                    Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
                    return null;
                } catch (OperationApplicationException e) {
                    // 若执行操作时发生操作应用异常，记录错误日志
                    Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
                    return null;
                }
            }
            return null;
        }
    }
}
