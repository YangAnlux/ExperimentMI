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

package net.micode.notes.tool;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
import android.util.Log;

import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.CallNote;
import net.micode.notes.data.Notes.NoteColumns;
import net.micode.notes.ui.NotesListAdapter.AppWidgetAttribute;

import java.util.ArrayList;
import java.util.HashSet;

// DataUtils 类提供了一系列用于操作笔记数据的工具方法，包括批量删除、移动笔记、查询文件夹数量等
public class DataUtils {
    // 日志标签，用于在日志中标识该类的相关信息
    public static final String TAG = "DataUtils";

    /**
     * 批量删除笔记的方法
     * @param resolver ContentResolver 对象，用于与内容提供者进行交互
     * @param ids 要删除的笔记 ID 的集合
     * @return 删除成功返回 true，失败返回 false
     */
    public static boolean batchDeleteNotes(ContentResolver resolver, HashSet<Long> ids) {
        // 若传入的 ID 集合为 null，记录日志并返回 true
        if (ids == null) {
            Log.d(TAG, "the ids is null");
            return true;
        }
        // 若 ID 集合为空，记录日志并返回 true
        if (ids.size() == 0) {
            Log.d(TAG, "no id is in the hashset");
            return true;
        }

        // 创建一个 ContentProviderOperation 列表，用于批量操作
        ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();
        for (long id : ids) {
            // 若 ID 为根文件夹的 ID，记录错误日志并跳过该 ID
            if (id == Notes.ID_ROOT_FOLDER) {
                Log.e(TAG, "Don't delete system folder root");
                continue;
            }
            // 创建一个删除操作的构建器
            ContentProviderOperation.Builder builder = ContentProviderOperation
                   .newDelete(ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, id));
            // 将构建好的操作添加到列表中
            operationList.add(builder.build());
        }
        try {
            // 应用批量操作
            ContentProviderResult[] results = resolver.applyBatch(Notes.AUTHORITY, operationList);
            // 若操作结果为空或长度为 0 或第一个结果为 null，记录日志并返回 false
            if (results == null || results.length == 0 || results[0] == null) {
                Log.d(TAG, "delete notes failed, ids:" + ids.toString());
                return false;
            }
            return true;
        } catch (RemoteException e) {
            // 捕获远程异常，记录错误日志
            Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
        } catch (OperationApplicationException e) {
            // 捕获操作应用异常，记录错误日志
            Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
        }
        return false;
    }

    /**
     * 将单个笔记移动到指定文件夹的方法
     * @param resolver ContentResolver 对象，用于与内容提供者进行交互
     * @param id 要移动的笔记的 ID
     * @param srcFolderId 笔记的源文件夹 ID
     * @param desFolderId 笔记要移动到的目标文件夹 ID
     */
    public static void moveNoteToFoler(ContentResolver resolver, long id, long srcFolderId, long desFolderId) {
        // 创建一个 ContentValues 对象，用于存储要更新的列和值
        ContentValues values = new ContentValues();
        // 设置笔记的父文件夹 ID 为目标文件夹 ID
        values.put(NoteColumns.PARENT_ID, desFolderId);
        // 设置笔记的原始父文件夹 ID 为源文件夹 ID
        values.put(NoteColumns.ORIGIN_PARENT_ID, srcFolderId);
        // 设置笔记的本地修改标志为 1，表示已修改
        values.put(NoteColumns.LOCAL_MODIFIED, 1);
        // 更新指定 ID 的笔记信息
        resolver.update(ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, id), values, null, null);
    }

    /**
     * 批量将笔记移动到指定文件夹的方法
     * @param resolver ContentResolver 对象，用于与内容提供者进行交互
     * @param ids 要移动的笔记 ID 的集合
     * @param folderId 目标文件夹的 ID
     * @return 移动成功返回 true，失败返回 false
     */
    public static boolean batchMoveToFolder(ContentResolver resolver, HashSet<Long> ids,
                                            long folderId) {
        // 若传入的 ID 集合为 null，记录日志并返回 true
        if (ids == null) {
            Log.d(TAG, "the ids is null");
            return true;
        }

        // 创建一个 ContentProviderOperation 列表，用于批量操作
        ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();
        for (long id : ids) {
            // 创建一个更新操作的构建器
            ContentProviderOperation.Builder builder = ContentProviderOperation
                   .newUpdate(ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, id));
            // 设置笔记的父文件夹 ID 为目标文件夹 ID
            builder.withValue(NoteColumns.PARENT_ID, folderId);
            // 设置笔记的本地修改标志为 1，表示已修改
            builder.withValue(NoteColumns.LOCAL_MODIFIED, 1);
            // 将构建好的操作添加到列表中
            operationList.add(builder.build());
        }

        try {
            // 应用批量操作
            ContentProviderResult[] results = resolver.applyBatch(Notes.AUTHORITY, operationList);
            // 若操作结果为空或长度为 0 或第一个结果为 null，记录日志并返回 false
            if (results == null || results.length == 0 || results[0] == null) {
                Log.d(TAG, "delete notes failed, ids:" + ids.toString());
                return false;
            }
            return true;
        } catch (RemoteException e) {
            // 捕获远程异常，记录错误日志
            Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
        } catch (OperationApplicationException e) {
            // 捕获操作应用异常，记录错误日志
            Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
        }
        return false;
    }

    /**
     * 获取用户文件夹数量的方法，排除系统文件夹
     * @param resolver ContentResolver 对象，用于与内容提供者进行交互
     * @return 用户文件夹的数量
     */
    public static int getUserFolderCount(ContentResolver resolver) {
        // 查询文件夹数量，排除回收站文件夹
        Cursor cursor = resolver.query(Notes.CONTENT_NOTE_URI,
                new String[]{"COUNT(*)"},
                NoteColumns.TYPE + "=? AND " + NoteColumns.PARENT_ID + "<>?",
                new String[]{String.valueOf(Notes.TYPE_FOLDER), String.valueOf(Notes.ID_TRASH_FOLER)},
                null);

        int count = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                try {
                    // 获取查询结果中的文件夹数量
                    count = cursor.getInt(0);
                } catch (IndexOutOfBoundsException e) {
                    // 捕获索引越界异常，记录错误日志
                    Log.e(TAG, "get folder count failed:" + e.toString());
                } finally {
                    // 关闭游标，释放资源
                    cursor.close();
                }
            }
        }
        return count;
    }

    /**
     * 检查笔记是否在笔记数据库中可见的方法
     * @param resolver ContentResolver 对象，用于与内容提供者进行交互
     * @param noteId 要检查的笔记的 ID
     * @param type 笔记的类型
     * @return 笔记可见返回 true，不可见返回 false
     */
    public static boolean visibleInNoteDatabase(ContentResolver resolver, long noteId, int type) {
        // 查询指定 ID 和类型的笔记，排除回收站中的笔记
        Cursor cursor = resolver.query(ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, noteId),
                null,
                NoteColumns.TYPE + "=? AND " + NoteColumns.PARENT_ID + "<>" + Notes.ID_TRASH_FOLER,
                new String[]{String.valueOf(type)},
                null);

        boolean exist = false;
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                // 若查询结果不为空，说明笔记可见
                exist = true;
            }
            // 关闭游标，释放资源
            cursor.close();
        }
        return exist;
    }

    /**
     * 检查笔记是否存在于笔记数据库中的方法
     * @param resolver ContentResolver 对象，用于与内容提供者进行交互
     * @param noteId 要检查的笔记的 ID
     * @return 笔记存在返回 true，不存在返回 false
     */
    public static boolean existInNoteDatabase(ContentResolver resolver, long noteId) {
        // 查询指定 ID 的笔记
        Cursor cursor = resolver.query(ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, noteId),
                null, null, null, null);

        boolean exist = false;
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                // 若查询结果不为空，说明笔记存在
                exist = true;
            }
            // 关闭游标，释放资源
            cursor.close();
        }
        return exist;
    }

    /**
     * 检查数据是否存在于数据数据库中的方法
     * @param resolver ContentResolver 对象，用于与内容提供者进行交互
     * @param dataId 要检查的数据的 ID
     * @return 数据存在返回 true，不存在返回 false
     */
    public static boolean existInDataDatabase(ContentResolver resolver, long dataId) {
        // 查询指定 ID 的数据
        Cursor cursor = resolver.query(ContentUris.withAppendedId(Notes.CONTENT_DATA_URI, dataId),
                null, null, null, null);

        boolean exist = false;
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                // 若查询结果不为空，说明数据存在
                exist = true;
            }
            // 关闭游标，释放资源
            cursor.close();
        }
        return exist;
    }

    /**
     * 检查可见文件夹名称是否已存在的方法
     * @param resolver ContentResolver 对象，用于与内容提供者进行交互
     * @param name 要检查的文件夹名称
     * @return 文件夹名称已存在返回 true，不存在返回 false
     */
    public static boolean checkVisibleFolderName(ContentResolver resolver, String name) {
        // 查询指定名称的可见文件夹，排除回收站中的文件夹
        Cursor cursor = resolver.query(Notes.CONTENT_NOTE_URI, null,
                NoteColumns.TYPE + "=" + Notes.TYPE_FOLDER +
                        " AND " + NoteColumns.PARENT_ID + "<>" + Notes.ID_TRASH_FOLER +
                        " AND " + NoteColumns.SNIPPET + "=?",
                new String[]{name}, null);
        boolean exist = false;
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                // 若查询结果不为空，说明文件夹名称已存在
                exist = true;
            }
            // 关闭游标，释放资源
            cursor.close();
        }
        return exist;
    }

    /**
     * 获取指定文件夹下笔记小部件属性集合的方法
     * @param resolver ContentResolver 对象，用于与内容提供者进行交互
     * @param folderId 文件夹的 ID
     * @return 小部件属性的集合
     */
    public static HashSet<AppWidgetAttribute> getFolderNoteWidget(ContentResolver resolver, long folderId) {
        // 查询指定文件夹下笔记的小部件 ID 和类型
        Cursor c = resolver.query(Notes.CONTENT_NOTE_URI,
                new String[]{NoteColumns.WIDGET_ID, NoteColumns.WIDGET_TYPE},
                NoteColumns.PARENT_ID + "=?",
                new String[]{String.valueOf(folderId)},
                null);

        HashSet<AppWidgetAttribute> set = null;
        if (c != null) {
            if (c.moveToFirst()) {
                // 创建一个 HashSet 用于存储小部件属性
                set = new HashSet<AppWidgetAttribute>();
                do {
                    try {
                        // 创建一个 AppWidgetAttribute 对象
                        AppWidgetAttribute widget = new AppWidgetAttribute();
                        // 获取小部件 ID
                        widget.widgetId = c.getInt(0);
                        // 获取小部件类型
                        widget.widgetType = c.getInt(1);
                        // 将小部件属性添加到集合中
                        set.add(widget);
                    } catch (IndexOutOfBoundsException e) {
                        // 捕获索引越界异常，记录错误日志
                        Log.e(TAG, e.toString());
                    }
                } while (c.moveToNext());
            }
            // 关闭游标，释放资源
            c.close();
        }
        return set;
    }

    /**
     * 根据笔记 ID 获取通话号码的方法
     * @param resolver ContentResolver 对象，用于与内容提供者进行交互
     * @param noteId 笔记的 ID
     * @return 通话号码，若未找到则返回空字符串
     */
    public static String getCallNumberByNoteId(ContentResolver resolver, long noteId) {
        // 查询指定笔记 ID 的通话号码
        Cursor cursor = resolver.query(Notes.CONTENT_DATA_URI,
                new String[]{CallNote.PHONE_NUMBER},
                CallNote.NOTE_ID + "=? AND " + CallNote.MIME_TYPE + "=?",
                new String[]{String.valueOf(noteId), CallNote.CONTENT_ITEM_TYPE},
                null);

        if (cursor != null && cursor.moveToFirst()) {
            try {
                // 获取查询结果中的通话号码
                return cursor.getString(0);
            } catch (IndexOutOfBoundsException e) {
                // 捕获索引越界异常，记录错误日志
                Log.e(TAG, "Get call number fails " + e.toString());
            } finally {
                // 关闭游标，释放资源
                cursor.close();
            }
        }
        return "";
    }

    /**
     * 根据电话号码和通话日期获取笔记 ID 的方法
     * @param resolver ContentResolver 对象，用于与内容提供者进行交互
     * @param phoneNumber 电话号码
     * @param callDate 通话日期
     * @return 笔记 ID，若未找到则返回 0
     */
    public static long getNoteIdByPhoneNumberAndCallDate(ContentResolver resolver, String phoneNumber, long callDate) {
        // 查询指定电话号码和通话日期的笔记 ID
        Cursor cursor = resolver.query(Notes.CONTENT_DATA_URI,
                new String[]{CallNote.NOTE_ID},
                CallNote.CALL_DATE + "=? AND " + CallNote.MIME_TYPE + "=? AND PHONE_NUMBERS_EQUAL("
                        + CallNote.PHONE_NUMBER + ",?)",
                new String[]{String.valueOf(callDate), CallNote.CONTENT_ITEM_TYPE, phoneNumber},
                null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                try {
                    // 获取查询结果中的笔记 ID
                    return cursor.getLong(0);
                } catch (IndexOutOfBoundsException e) {
                    // 捕获索引越界异常，记录错误日志
                    Log.e(TAG, "Get call note id fails " + e.toString());
                }
            }
            // 关闭游标，释放资源
            cursor.close();
        }
        return 0;
    }

    /**
     * 根据笔记 ID 获取笔记摘要的方法
     * @param resolver ContentResolver 对象，用于与内容提供者进行交互
     * @param noteId 笔记的 ID
     * @return 笔记摘要，若未找到则抛出 IllegalArgumentException 异常
     */
    public static String getSnippetById(ContentResolver resolver, long noteId) {
        // 查询指定笔记 ID 的摘要
        Cursor cursor = resolver.query(Notes.CONTENT_NOTE_URI,
                new String[]{NoteColumns.SNIPPET},
                NoteColumns.ID + "=?",
                new String[]{String.valueOf(noteId)},
                null);

        if (cursor != null) {
            String snippet = "";
            if (cursor.moveToFirst()) {
                // 获取查询结果中的笔记摘要
                snippet = cursor.getString(0);
            }
            // 关闭游标，释放资源
            cursor.close();
            return snippet;
        }
        // 若未找到笔记，抛出异常
        throw new IllegalArgumentException("Note is not found with id: " + noteId);
    }

    /**
     * 格式化笔记摘要的方法，去除首尾空格并截取第一行
     * @param snippet 要格式化的笔记摘要
     * @return 格式化后的笔记摘要
     */
    public static String getFormattedSnippet(String snippet) {
        if (snippet != null) {
            // 去除首尾空格
            snippet = snippet.trim();
            // 查找换行符的位置
            int index = snippet.indexOf('\n');
            if (index != -1) {
                // 若存在换行符，截取第一行
                snippet = snippet.substring(0, index);
            }
        }
        return snippet;
    }
}
