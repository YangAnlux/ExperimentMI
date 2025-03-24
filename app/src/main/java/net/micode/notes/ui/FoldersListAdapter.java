/*
 * Copyright (c) 2010-2011, The MiCode Open Source Community (www.micode.net)
 *
 * Licensed under the Apache License...（保留原有版权信息）
 */

package net.micode.notes.ui;

// 模块/库导入注释 ======================================================
import android.content.Context;      // 上下文环境
import android.database.Cursor;     // 数据库游标
import android.view.View;           // 视图基类
import android.view.ViewGroup;      // 视图容器
import android.widget.CursorAdapter; // 游标适配器基类
import android.widget.LinearLayout; // 线性布局
import android.widget.TextView;     // 文本显示控件

import net.micode.notes.R;          // 资源文件
import net.micode.notes.data.Notes; // 便签数据契约类
import net.micode.notes.data.Notes.NoteColumns; // 便签列定义

// 类注释 =============================================================
/**
 * 文件夹列表适配器
 * 功能：管理文件夹列表的数据展示，主要特性包括：
 * 1. 基于Cursor加载文件夹数据
 * 2. 特殊处理根文件夹显示名称
 * 3. 自定义列表项视图
 * 
 * 使用场景：在文件管理/文件夹选择界面展示可用文件夹列表
 */
public class FoldersListAdapter extends CursorAdapter {
    // 数据库查询相关配置 ================================================
    public static final String [] PROJECTION = {
        NoteColumns.ID,       // 文件夹ID列
        NoteColumns.SNIPPET   // 文件夹名称/片段列
    };

    // 列索引常量
    public static final int ID_COLUMN   = 0; // ID列索引
    public static final int NAME_COLUMN = 1; // 名称列索引

    // 构造方法 ========================================================
    /**
     * 初始化文件夹列表适配器
     * @param context 上下文环境（通常为Activity）
     * @param c 数据游标（需包含PROJECTION指定列）
     * 
     * 注意事项：传入的Cursor必须包含ID和SNIPPET两列
     */
    public FoldersListAdapter(Context context, Cursor c) {
        super(context, c, FLAG_REGISTER_CONTENT_OBSERVER);
    }

    // 视图创建与绑定 ==================================================
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        // >>> 关键算法：创建列表项视图
        return new FolderListItem(context);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // >>> 关键算法：绑定数据到视图
        if (view instanceof FolderListItem) {
            String folderName;
            long folderId = cursor.getLong(ID_COLUMN);
            
            // 边界条件：根文件夹特殊处理
            if (folderId == Notes.ID_ROOT_FOLDER) {
                folderName = context.getString(R.string.menu_move_parent_folder);
            } else {
                folderName = cursor.getString(NAME_COLUMN);
                // 最佳实践提示：此处应增加空值判断
                if (folderName == null) folderName = "";
            }
            
            ((FolderListItem) view).bind(folderName);
        }
    }

    // 数据获取方法 ====================================================
    /**
     * 获取指定位置的文件夹显示名称
     * @param context 上下文（用于获取资源字符串）
     * @param position 列表位置
     * @return 格式化后的文件夹名称
     * 
     * 边界条件说明：
     * - 当position越界时将抛出异常
     * - 根文件夹返回预设的父文件夹名称
     */
    public String getFolderName(Context context, int position) {
        Cursor cursor = (Cursor) getItem(position);
        long folderId = cursor.getLong(ID_COLUMN);
        return (folderId == Notes.ID_ROOT_FOLDER) 
                ? context.getString(R.string.menu_move_parent_folder)
                : cursor.getString(NAME_COLUMN);
    }

    // 自定义列表项视图 =================================================
    /**
     * 文件夹列表项自定义视图
     * 布局文件：R.layout.folder_list_item
     * 包含元素：显示文件夹名称的TextView（id: tv_folder_name）
     */
    private class FolderListItem extends LinearLayout {
        private TextView mName; // 名称显示控件

        public FolderListItem(Context context) {
            super(context);
            // 初始化布局
            inflate(context, R.layout.folder_list_item, this);
            mName = (TextView) findViewById(R.id.tv_folder_name);
            
            // 最佳实践提示：应添加以下属性设置
            setOrientation(HORIZONTAL);
            setBackgroundResource(android.R.drawable.list_selector_background);
        }

        /**
         * 绑定文件夹名称到视图
         * @param name 要显示的文件夹名称
         */
        public void bind(String name) {
            mName.setText(name);
            // 最佳实践提示：可添加文件夹图标等扩展功能
            // mName.setCompoundDrawablesWithIntrinsicBounds(R.drawable.folder_icon, 0, 0, 0);
        }
    }
}
