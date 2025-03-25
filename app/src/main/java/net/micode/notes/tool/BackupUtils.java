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

import android.content.Context;
import android.database.Cursor;
import android.os.Environment;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;

import net.micode.notes.R;
import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.DataColumns;
import net.micode.notes.data.Notes.DataConstants;
import net.micode.notes.data.Notes.NoteColumns;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

// BackupUtils 类用于将笔记数据备份为文本文件，提供了单例模式和状态码来表示备份操作的结果
public class BackupUtils {
    // 日志标签，用于在日志中标识该类的相关信息
    private static final String TAG = "BackupUtils";
    // 单例实例
    private static BackupUtils sInstance;

    /**
     * 获取 BackupUtils 的单例实例
     * @param context 上下文对象
     * @return BackupUtils 的单例实例
     */
    public static synchronized BackupUtils getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new BackupUtils(context);
        }
        return sInstance;
    }

    /**
     * 以下状态码用于表示备份或恢复操作的状态
     */
    // 当前 SD 卡未挂载
    public static final int STATE_SD_CARD_UNMOUONTED = 0;
    // 备份文件不存在
    public static final int STATE_BACKUP_FILE_NOT_EXIST = 1;
    // 数据格式不正确，可能被其他程序修改
    public static final int STATE_DATA_DESTROIED = 2;
    // 运行时异常导致备份或恢复失败
    public static final int STATE_SYSTEM_ERROR = 3;
    // 备份或恢复成功
    public static final int STATE_SUCCESS = 4;

    // 文本导出工具类实例
    private TextExport mTextExport;

    /**
     * 私有构造函数，初始化 TextExport 实例
     * @param context 上下文对象
     */
    private BackupUtils(Context context) {
        mTextExport = new TextExport(context);
    }

    /**
     * 检查外部存储是否可用
     * @return 可用返回 true，不可用返回 false
     */
    private static boolean externalStorageAvailable() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    /**
     * 导出笔记数据为文本文件
     * @return 导出操作的状态码
     */
    public int exportToText() {
        return mTextExport.exportToText();
    }

    /**
     * 获取导出的文本文件的文件名
     * @return 文件名
     */
    public String getExportedTextFileName() {
        return mTextExport.mFileName;
    }

    /**
     * 获取导出的文本文件的目录
     * @return 文件目录
     */
    public String getExportedTextFileDir() {
        return mTextExport.mFileDirectory;
    }

    // 内部类，用于将笔记数据导出为文本文件
    private static class TextExport {
        // 查询笔记信息时使用的投影列
        private static final String[] NOTE_PROJECTION = {
                NoteColumns.ID,
                NoteColumns.MODIFIED_DATE,
                NoteColumns.SNIPPET,
                NoteColumns.TYPE
        };

        // NOTE_PROJECTION 数组中 ID 列的索引
        private static final int NOTE_COLUMN_ID = 0;
        // NOTE_PROJECTION 数组中修改日期列的索引
        private static final int NOTE_COLUMN_MODIFIED_DATE = 1;
        // NOTE_PROJECTION 数组中摘要列的索引
        private static final int NOTE_COLUMN_SNIPPET = 2;

        // 查询笔记数据时使用的投影列
        private static final String[] DATA_PROJECTION = {
                DataColumns.CONTENT,
                DataColumns.MIME_TYPE,
                DataColumns.DATA1,
                DataColumns.DATA2,
                DataColumns.DATA3,
                DataColumns.DATA4,
        };

        // DATA_PROJECTION 数组中内容列的索引
        private static final int DATA_COLUMN_CONTENT = 0;
        // DATA_PROJECTION 数组中 MIME 类型列的索引
        private static final int DATA_COLUMN_MIME_TYPE = 1;
        // DATA_PROJECTION 数组中通话日期列的索引
        private static final int DATA_COLUMN_CALL_DATE = 2;
        // DATA_PROJECTION 数组中电话号码列的索引
        private static final int DATA_COLUMN_PHONE_NUMBER = 4;

        // 导出文本的格式数组
        private final String[] TEXT_FORMAT;
        // 文件夹名称格式的索引
        private static final int FORMAT_FOLDER_NAME = 0;
        // 笔记日期格式的索引
        private static final int FORMAT_NOTE_DATE = 1;
        // 笔记内容格式的索引
        private static final int FORMAT_NOTE_CONTENT = 2;

        // 上下文对象
        private Context mContext;
        // 导出的文本文件名
        private String mFileName;
        // 导出的文本文件目录
        private String mFileDirectory;

        /**
         * 构造函数，初始化上下文、文件名和文件目录，并获取导出文本的格式数组
         * @param context 上下文对象
         */
        public TextExport(Context context) {
            TEXT_FORMAT = context.getResources().getStringArray(R.array.format_for_exported_note);
            mContext = context;
            mFileName = "";
            mFileDirectory = "";
        }

        /**
         * 根据索引获取导出文本的格式字符串
         * @param id 格式数组的索引
         * @return 格式字符串
         */
        private String getFormat(int id) {
            return TEXT_FORMAT[id];
        }

        /**
         * 将指定文件夹下的笔记导出为文本
         * @param folderId 文件夹 ID
         * @param ps 打印流，用于将数据写入文件
         */
        private void exportFolderToText(String folderId, PrintStream ps) {
            // 查询属于该文件夹的笔记
            Cursor notesCursor = mContext.getContentResolver().query(Notes.CONTENT_NOTE_URI,
                    NOTE_PROJECTION, NoteColumns.PARENT_ID + "=?", new String[]{
                            folderId
                    }, null);

            if (notesCursor != null) {
                if (notesCursor.moveToFirst()) {
                    do {
                        // 打印笔记的最后修改日期
                        ps.println(String.format(getFormat(FORMAT_NOTE_DATE), DateFormat.format(
                                mContext.getString(R.string.format_datetime_mdhm),
                                notesCursor.getLong(NOTE_COLUMN_MODIFIED_DATE))));
                        // 查询属于该笔记的数据
                        String noteId = notesCursor.getString(NOTE_COLUMN_ID);
                        exportNoteToText(noteId, ps);
                    } while (notesCursor.moveToNext());
                }
                // 关闭游标，释放资源
                notesCursor.close();
            }
        }

        /**
         * 将指定 ID 的笔记导出为文本
         * @param noteId 笔记 ID
         * @param ps 打印流，用于将数据写入文件
         */
        private void exportNoteToText(String noteId, PrintStream ps) {
            // 查询属于该笔记的数据
            Cursor dataCursor = mContext.getContentResolver().query(Notes.CONTENT_DATA_URI,
                    DATA_PROJECTION, DataColumns.NOTE_ID + "=?", new String[]{
                            noteId
                    }, null);

            if (dataCursor != null) {
                if (dataCursor.moveToFirst()) {
                    do {
                        // 获取数据的 MIME 类型
                        String mimeType = dataCursor.getString(DATA_COLUMN_MIME_TYPE);
                        if (DataConstants.CALL_NOTE.equals(mimeType)) {
                            // 若为通话笔记类型
                            // 打印电话号码
                            String phoneNumber = dataCursor.getString(DATA_COLUMN_PHONE_NUMBER);
                            long callDate = dataCursor.getLong(DATA_COLUMN_CALL_DATE);
                            String location = dataCursor.getString(DATA_COLUMN_CONTENT);

                            if (!TextUtils.isEmpty(phoneNumber)) {
                                ps.println(String.format(getFormat(FORMAT_NOTE_CONTENT),
                                        phoneNumber));
                            }
                            // 打印通话日期
                            ps.println(String.format(getFormat(FORMAT_NOTE_CONTENT), DateFormat
                                    .format(mContext.getString(R.string.format_datetime_mdhm),
                                            callDate)));
                            // 打印通话附件位置
                            if (!TextUtils.isEmpty(location)) {
                                ps.println(String.format(getFormat(FORMAT_NOTE_CONTENT),
                                        location));
                            }
                        } else if (DataConstants.NOTE.equals(mimeType)) {
                            // 若为普通笔记类型
                            String content = dataCursor.getString(DATA_COLUMN_CONTENT);
                            if (!TextUtils.isEmpty(content)) {
                                ps.println(String.format(getFormat(FORMAT_NOTE_CONTENT),
                                        content));
                            }
                        }
                    } while (dataCursor.moveToNext());
                }
                // 关闭游标，释放资源
                dataCursor.close();
            }
            // 在笔记之间打印换行符
            try {
                ps.write(new byte[]{
                        Character.LINE_SEPARATOR, Character.LETTER_NUMBER
                });
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            }
        }

        /**
         * 将笔记数据导出为用户可读的文本文件
         * @return 导出操作的状态码
         */
        public int exportToText() {
            if (!externalStorageAvailable()) {
                // 若外部存储不可用，记录日志并返回相应状态码
                Log.d(TAG, "Media was not mounted");
                return STATE_SD_CARD_UNMOUONTED;
            }

            PrintStream ps = getExportToTextPrintStream();
            if (ps == null) {
                // 若获取打印流失败，记录日志并返回相应状态码
                Log.e(TAG, "get print stream error");
                return STATE_SYSTEM_ERROR;
            }
            // 首先导出文件夹及其包含的笔记
            Cursor folderCursor = mContext.getContentResolver().query(
                    Notes.CONTENT_NOTE_URI,
                    NOTE_PROJECTION,
                    "(" + NoteColumns.TYPE + "=" + Notes.TYPE_FOLDER + " AND "
                            + NoteColumns.PARENT_ID + "<>" + Notes.ID_TRASH_FOLER + ") OR "
                            + NoteColumns.ID + "=" + Notes.ID_CALL_RECORD_FOLDER, null, null);

            if (folderCursor != null) {
                if (folderCursor.moveToFirst()) {
                    do {
                        // 打印文件夹名称
                        String folderName = "";
                        if (folderCursor.getLong(NOTE_COLUMN_ID) == Notes.ID_CALL_RECORD_FOLDER) {
                            folderName = mContext.getString(R.string.call_record_folder_name);
                        } else {
                            folderName = folderCursor.getString(NOTE_COLUMN_SNIPPET);
                        }
                        if (!TextUtils.isEmpty(folderName)) {
                            ps.println(String.format(getFormat(FORMAT_FOLDER_NAME), folderName));
                        }
                        String folderId = folderCursor.getString(NOTE_COLUMN_ID);
                        exportFolderToText(folderId, ps);
                    } while (folderCursor.moveToNext());
                }
                // 关闭游标，释放资源
                folderCursor.close();
            }

            // 导出根文件夹下的笔记
            Cursor noteCursor = mContext.getContentResolver().query(
                    Notes.CONTENT_NOTE_URI,
                    NOTE_PROJECTION,
                    NoteColumns.TYPE + "=" + Notes.TYPE_NOTE + " AND " + NoteColumns.PARENT_ID
                            + "=0", null, null);

            if (noteCursor != null) {
                if (noteCursor.moveToFirst()) {
                    do {
                        ps.println(String.format(getFormat(FORMAT_NOTE_DATE), DateFormat.format(
                                mContext.getString(R.string.format_datetime_mdhm),
                                noteCursor.getLong(NOTE_COLUMN_MODIFIED_DATE))));
                        // 查询属于该笔记的数据
                        String noteId = noteCursor.getString(NOTE_COLUMN_ID);
                        exportNoteToText(noteId, ps);
                    } while (noteCursor.moveToNext());
                }
                // 关闭游标，释放资源
                noteCursor.close();
            }
            // 关闭打印流
            ps.close();

            return STATE_SUCCESS;
        }

        /**
         * 获取指向导出文本文件的打印流
         * @return 打印流对象，若获取失败返回 null
         */
        private PrintStream getExportToTextPrintStream() {
            File file = generateFileMountedOnSDcard(mContext, R.string.file_path,
                    R.string.file_name_txt_format);
            if (file == null) {
                // 若生成文件失败，记录日志并返回 null
                Log.e(TAG, "create file to exported failed");
                return null;
            }
            mFileName = file.getName();
            mFileDirectory = mContext.getString(R.string.file_path);
            PrintStream ps = null;
            try {
                FileOutputStream fos = new FileOutputStream(file);
                ps = new PrintStream(fos);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return null;
            } catch (NullPointerException e) {
                e.printStackTrace();
                return null;
            }
            return ps;
        }
    }

    /**
     * 生成存储导出数据的文本文件
     * @param context 上下文对象
     * @param filePathResId 文件路径的资源 ID
     * @param fileNameFormatResId 文件名格式的资源 ID
     * @return 生成的文件对象，若生成失败返回 null
     */
    private static File generateFileMountedOnSDcard(Context context, int filePathResId, int fileNameFormatResId) {
        StringBuilder sb = new StringBuilder();
        sb.append(Environment.getExternalStorageDirectory());
        sb.append(context.getString(filePathResId));
        File filedir = new File(sb.toString());
        sb.append(context.getString(
                fileNameFormatResId,
                DateFormat.format(context.getString(R.string.format_date_ymd),
                        System.currentTimeMillis())));
        File file = new File(sb.toString());

        try {
            if (!filedir.exists()) {
                filedir.mkdir();
            }
            if (!file.exists()) {
                file.createNewFile();
            }
            return file;
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
