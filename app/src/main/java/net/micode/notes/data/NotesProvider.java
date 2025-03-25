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

package net.micode.notes.data;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import net.micode.notes.R;
import net.micode.notes.data.Notes.DataColumns;
import net.micode.notes.data.Notes.NoteColumns;
import net.micode.notes.data.NotesDatabaseHelper.TABLE;

// 自定义的 ContentProvider，用于管理笔记数据的增删改查操作
public class NotesProvider extends ContentProvider {
    // UriMatcher 用于匹配不同的 URI，确定具体的操作类型
    private static final UriMatcher mMatcher;
    // 数据库帮助类的实例，用于操作数据库
    private NotesDatabaseHelper mHelper;
    // 日志标签，用于标识该类的日志信息
    private static final String TAG = "NotesProvider";

    // 定义不同 URI 匹配结果的常量
    private static final int URI_NOTE = 1;
    private static final int URI_NOTE_ITEM = 2;
    private static final int URI_DATA = 3;
    private static final int URI_DATA_ITEM = 4;
    private static final int URI_SEARCH = 5;
    private static final int URI_SEARCH_SUGGEST = 6;

    // 静态代码块，在类加载时初始化 UriMatcher
    static {
        mMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        // 匹配查询所有笔记的 URI
        mMatcher.addURI(Notes.AUTHORITY, "note", URI_NOTE);
        // 匹配查询单个笔记的 URI
        mMatcher.addURI(Notes.AUTHORITY, "note/#", URI_NOTE_ITEM);
        // 匹配查询所有数据的 URI
        mMatcher.addURI(Notes.AUTHORITY, "data", URI_DATA);
        // 匹配查询单个数据的 URI
        mMatcher.addURI(Notes.AUTHORITY, "data/#", URI_DATA_ITEM);
        // 匹配搜索笔记的 URI
        mMatcher.addURI(Notes.AUTHORITY, "search", URI_SEARCH);
        // 匹配搜索建议的 URI
        mMatcher.addURI(Notes.AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY, URI_SEARCH_SUGGEST);
        mMatcher.addURI(Notes.AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY + "/*", URI_SEARCH_SUGGEST);
    }

    /**
     * x'0A' 表示 SQLite 中的 '\n' 字符。对于搜索结果中的标题和内容，
     * 我们将去除 '\n' 和空白字符，以显示更多信息。
     */
    private static final String NOTES_SEARCH_PROJECTION = NoteColumns.ID + ","
            + NoteColumns.ID + " AS " + SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA + ","
            + "TRIM(REPLACE(" + NoteColumns.SNIPPET + ", x'0A','')) AS " + SearchManager.SUGGEST_COLUMN_TEXT_1 + ","
            + "TRIM(REPLACE(" + NoteColumns.SNIPPET + ", x'0A','')) AS " + SearchManager.SUGGEST_COLUMN_TEXT_2 + ","
            + R.drawable.search_result + " AS " + SearchManager.SUGGEST_COLUMN_ICON_1 + ","
            + "'" + Intent.ACTION_VIEW + "' AS " + SearchManager.SUGGEST_COLUMN_INTENT_ACTION + ","
            + "'" + Notes.TextNote.CONTENT_TYPE + "' AS " + SearchManager.SUGGEST_COLUMN_INTENT_DATA;

    // 笔记搜索的 SQL 查询语句
    private static String NOTES_SNIPPET_SEARCH_QUERY = "SELECT " + NOTES_SEARCH_PROJECTION
            + " FROM " + TABLE.NOTE
            + " WHERE " + NoteColumns.SNIPPET + " LIKE ?"
            + " AND " + NoteColumns.PARENT_ID + "<>" + Notes.ID_TRASH_FOLER
            + " AND " + NoteColumns.TYPE + "=" + Notes.TYPE_NOTE;

    // 初始化 ContentProvider 时调用的方法
    @Override
    public boolean onCreate() {
        // 获取数据库帮助类的单例实例
        mHelper = NotesDatabaseHelper.getInstance(getContext());
        return true;
    }

    // 查询数据的方法
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        Cursor c = null;
        // 获取可读的数据库实例
        SQLiteDatabase db = mHelper.getReadableDatabase();
        String id = null;
        // 根据 URI 匹配结果执行不同的查询操作
        switch (mMatcher.match(uri)) {
            case URI_NOTE:
                // 查询所有笔记
                c = db.query(TABLE.NOTE, projection, selection, selectionArgs, null, null,
                        sortOrder);
                break;
            case URI_NOTE_ITEM:
                // 获取笔记的 ID
                id = uri.getPathSegments().get(1);
                // 查询单个笔记
                c = db.query(TABLE.NOTE, projection, NoteColumns.ID + "=" + id
                        + parseSelection(selection), selectionArgs, null, null, sortOrder);
                break;
            case URI_DATA:
                // 查询所有数据
                c = db.query(TABLE.DATA, projection, selection, selectionArgs, null, null,
                        sortOrder);
                break;
            case URI_DATA_ITEM:
                // 获取数据的 ID
                id = uri.getPathSegments().get(1);
                // 查询单个数据
                c = db.query(TABLE.DATA, projection, DataColumns.ID + "=" + id
                        + parseSelection(selection), selectionArgs, null, null, sortOrder);
                break;
            case URI_SEARCH:
            case URI_SEARCH_SUGGEST:
                // 搜索操作不允许指定排序和投影
                if (sortOrder != null || projection != null) {
                    throw new IllegalArgumentException(
                            "do not specify sortOrder, selection, selectionArgs, or projection" + "with this query");
                }

                String searchString = null;
                if (mMatcher.match(uri) == URI_SEARCH_SUGGEST) {
                    if (uri.getPathSegments().size() > 1) {
                        // 从 URI 中获取搜索字符串
                        searchString = uri.getPathSegments().get(1);
                    }
                } else {
                    // 从查询参数中获取搜索字符串
                    searchString = uri.getQueryParameter("pattern");
                }

                if (TextUtils.isEmpty(searchString)) {
                    return null;
                }

                try {
                    // 构造模糊搜索的字符串
                    searchString = String.format("%%%s%%", searchString);
                    // 执行搜索查询
                    c = db.rawQuery(NOTES_SNIPPET_SEARCH_QUERY,
                            new String[]{searchString});
                } catch (IllegalStateException ex) {
                    Log.e(TAG, "got exception: " + ex.toString());
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        if (c != null) {
            // 设置通知 URI，当数据变化时通知观察者
            c.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return c;
    }

    // 插入数据的方法
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        long dataId = 0, noteId = 0, insertedId = 0;
        // 根据 URI 匹配结果执行不同的插入操作
        switch (mMatcher.match(uri)) {
            case URI_NOTE:
                // 插入笔记
                insertedId = noteId = db.insert(TABLE.NOTE, null, values);
                break;
            case URI_DATA:
                if (values.containsKey(DataColumns.NOTE_ID)) {
                    // 获取数据所属的笔记 ID
                    noteId = values.getAsLong(DataColumns.NOTE_ID);
                } else {
                    Log.d(TAG, "Wrong data format without note id:" + values.toString());
                }
                // 插入数据
                insertedId = dataId = db.insert(TABLE.DATA, null, values);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        // 通知笔记 URI 数据发生变化
        if (noteId > 0) {
            getContext().getContentResolver().notifyChange(
                    ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, noteId), null);
        }

        // 通知数据 URI 数据发生变化
        if (dataId > 0) {
            getContext().getContentResolver().notifyChange(
                    ContentUris.withAppendedId(Notes.CONTENT_DATA_URI, dataId), null);
        }

        return ContentUris.withAppendedId(uri, insertedId);
    }

    // 删除数据的方法
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int count = 0;
        String id = null;
        // 获取可写的数据库实例
        SQLiteDatabase db = mHelper.getWritableDatabase();
        boolean deleteData = false;
        // 根据 URI 匹配结果执行不同的删除操作
        switch (mMatcher.match(uri)) {
            case URI_NOTE:
                // 确保删除的笔记 ID 大于 0
                selection = "(" + selection + ") AND " + NoteColumns.ID + ">0 ";
                // 删除笔记
                count = db.delete(TABLE.NOTE, selection, selectionArgs);
                break;
            case URI_NOTE_ITEM:
                // 获取笔记的 ID
                id = uri.getPathSegments().get(1);
                /**
                 * ID 小于 0 的是系统文件夹，不允许删除
                 */
                long noteId = Long.valueOf(id);
                if (noteId <= 0) {
                    break;
                }
                // 删除单个笔记
                count = db.delete(TABLE.NOTE,
                        NoteColumns.ID + "=" + id + parseSelection(selection), selectionArgs);
                break;
            case URI_DATA:
                // 删除数据
                count = db.delete(TABLE.DATA, selection, selectionArgs);
                deleteData = true;
                break;
            case URI_DATA_ITEM:
                // 获取数据的 ID
                id = uri.getPathSegments().get(1);
                // 删除单个数据
                count = db.delete(TABLE.DATA,
                        DataColumns.ID + "=" + id + parseSelection(selection), selectionArgs);
                deleteData = true;
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        if (count > 0) {
            if (deleteData) {
                // 通知笔记 URI 数据发生变化
                getContext().getContentResolver().notifyChange(Notes.CONTENT_NOTE_URI, null);
            }
            // 通知当前 URI 数据发生变化
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    // 更新数据的方法
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int count = 0;
        String id = null;
        // 获取可写的数据库实例
        SQLiteDatabase db = mHelper.getWritableDatabase();
        boolean updateData = false;
        // 根据 URI 匹配结果执行不同的更新操作
        switch (mMatcher.match(uri)) {
            case URI_NOTE:
                // 增加笔记的版本号
                increaseNoteVersion(-1, selection, selectionArgs);
                // 更新笔记
                count = db.update(TABLE.NOTE, values, selection, selectionArgs);
                break;
            case URI_NOTE_ITEM:
                // 获取笔记的 ID
                id = uri.getPathSegments().get(1);
                // 增加笔记的版本号
                increaseNoteVersion(Long.valueOf(id), selection, selectionArgs);
                // 更新单个笔记
                count = db.update(TABLE.NOTE, values, NoteColumns.ID + "=" + id
                        + parseSelection(selection), selectionArgs);
                break;
            case URI_DATA:
                // 更新数据
                count = db.update(TABLE.DATA, values, selection, selectionArgs);
                updateData = true;
                break;
            case URI_DATA_ITEM:
                // 获取数据的 ID
                id = uri.getPathSegments().get(1);
                // 更新单个数据
                count = db.update(TABLE.DATA, values, DataColumns.ID + "=" + id
                        + parseSelection(selection), selectionArgs);
                updateData = true;
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        if (count > 0) {
            if (updateData) {
                // 通知笔记 URI 数据发生变化
                getContext().getContentResolver().notifyChange(Notes.CONTENT_NOTE_URI, null);
            }
            // 通知当前 URI 数据发生变化
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    // 解析选择条件的方法，用于拼接 SQL 语句
    private String parseSelection(String selection) {
        return (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : "");
    }

    // 增加笔记版本号的方法
    private void increaseNoteVersion(long id, String selection, String[] selectionArgs) {
        StringBuilder sql = new StringBuilder(120);
        sql.append("UPDATE ");
        sql.append(TABLE.NOTE);
        sql.append(" SET ");
        sql.append(NoteColumns.VERSION);
        sql.append("=" + NoteColumns.VERSION + "+1 ");

        if (id > 0 || !TextUtils.isEmpty(selection)) {
            sql.append(" WHERE ");
        }
        if (id > 0) {
            sql.append(NoteColumns.ID + "=" + String.valueOf(id));
        }
        if (!TextUtils.isEmpty(selection)) {
            String selectString = id > 0 ? parseSelection(selection) : selection;
            for (String args : selectionArgs) {
                selectString = selectString.replaceFirst("\\?", args);
            }
            sql.append(selectString);
        }

        // 执行 SQL 语句增加笔记版本号
        mHelper.getWritableDatabase().execSQL(sql.toString());
    }

    // 获取数据类型的方法，目前未实现
    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }
}
