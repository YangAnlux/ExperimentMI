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
import android.text.format.DateUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.micode.notes.R;
import net.micode.notes.data.Notes;
import net.micode.notes.tool.DataUtils;
import net.micode.notes.tool.ResourceParser.NoteItemBgResources;

// NotesListItem类继承自LinearLayout，用于展示笔记列表中的每一项
public class NotesListItem extends LinearLayout {
    // 提醒图标ImageView
    private ImageView mAlert;
    // 标题TextView
    private TextView mTitle;
    // 时间TextView
    private TextView mTime;
    // 通话记录中的联系人姓名TextView
    private TextView mCallName;
    // 存储当前笔记项的数据
    private NoteItemData mItemData;
    // 多选模式下的复选框
    private CheckBox mCheckBox;

    // 构造函数，用于初始化视图组件
    public NotesListItem(Context context) {
        super(context);
        // 加载note_item布局文件到当前LinearLayout中
        inflate(context, R.layout.note_item, this);
        // 查找提醒图标ImageView
        mAlert = (ImageView) findViewById(R.id.iv_alert_icon);
        // 查找标题TextView
        mTitle = (TextView) findViewById(R.id.tv_title);
        // 查找时间TextView
        mTime = (TextView) findViewById(R.id.tv_time);
        // 查找通话记录中的联系人姓名TextView
        mCallName = (TextView) findViewById(R.id.tv_name);
        // 查找复选框
        mCheckBox = (CheckBox) findViewById(android.R.id.checkbox);
    }

    // 绑定数据到视图的方法
    public void bind(Context context, NoteItemData data, boolean choiceMode, boolean checked) {
        // 如果处于多选模式且当前项为笔记类型
        if (choiceMode && data.getType() == Notes.TYPE_NOTE) {
            // 显示复选框
            mCheckBox.setVisibility(View.VISIBLE);
            // 设置复选框的选中状态
            mCheckBox.setChecked(checked);
        } else {
            // 隐藏复选框
            mCheckBox.setVisibility(View.GONE);
        }

        // 保存当前笔记项的数据
        mItemData = data;
        // 如果当前项是通话记录文件夹
        if (data.getId() == Notes.ID_CALL_RECORD_FOLDER) {
            // 隐藏通话记录中的联系人姓名TextView
            mCallName.setVisibility(View.GONE);
            // 显示提醒图标ImageView
            mAlert.setVisibility(View.VISIBLE);
            // 设置标题的文字样式
            mTitle.setTextAppearance(context, R.style.TextAppearancePrimaryItem);
            // 设置标题文字，显示通话记录文件夹名称和其中的笔记数量
            mTitle.setText(context.getString(R.string.call_record_folder_name)
                    + context.getString(R.string.format_folder_files_count, data.getNotesCount()));
            // 设置提醒图标为通话记录图标
            mAlert.setImageResource(R.drawable.call_record);
        // 如果当前项的父文件夹是通话记录文件夹
        } else if (data.getParentId() == Notes.ID_CALL_RECORD_FOLDER) {
            // 显示通话记录中的联系人姓名TextView
            mCallName.setVisibility(View.VISIBLE);
            // 设置通话记录中的联系人姓名
            mCallName.setText(data.getCallName());
            // 设置标题的文字样式
            mTitle.setTextAppearance(context, R.style.TextAppearanceSecondaryItem);
            // 设置标题文字，显示格式化后的摘要
            mTitle.setText(DataUtils.getFormattedSnippet(data.getSnippet()));
            // 如果当前项有提醒
            if (data.hasAlert()) {
                // 设置提醒图标为时钟图标
                mAlert.setImageResource(R.drawable.clock);
                // 显示提醒图标ImageView
                mAlert.setVisibility(View.VISIBLE);
            } else {
                // 隐藏提醒图标ImageView
                mAlert.setVisibility(View.GONE);
            }
        } else {
            // 隐藏通话记录中的联系人姓名TextView
            mCallName.setVisibility(View.GONE);
            // 设置标题的文字样式
            mTitle.setTextAppearance(context, R.style.TextAppearancePrimaryItem);

            // 如果当前项是文件夹类型
            if (data.getType() == Notes.TYPE_FOLDER) {
                // 设置标题文字，显示文件夹名称和其中的笔记数量
                mTitle.setText(data.getSnippet()
                        + context.getString(R.string.format_folder_files_count,
                                data.getNotesCount()));
                // 隐藏提醒图标ImageView
                mAlert.setVisibility(View.GONE);
            } else {
                // 设置标题文字，显示格式化后的摘要
                mTitle.setText(DataUtils.getFormattedSnippet(data.getSnippet()));
                // 如果当前项有提醒
                if (data.hasAlert()) {
                    // 设置提醒图标为时钟图标
                    mAlert.setImageResource(R.drawable.clock);
                    // 显示提醒图标ImageView
                    mAlert.setVisibility(View.VISIBLE);
                } else {
                    // 隐藏提醒图标ImageView
                    mAlert.setVisibility(View.GONE);
                }
            }
        }
        // 设置时间TextView的文字，显示相对时间
        mTime.setText(DateUtils.getRelativeTimeSpanString(data.getModifiedDate()));

        // 设置当前项的背景
        setBackground(data);
    }

    // 设置背景的方法
    private void setBackground(NoteItemData data) {
        // 获取背景颜色ID
        int id = data.getBgColorId();
        // 如果当前项是笔记类型
        if (data.getType() == Notes.TYPE_NOTE) {
            // 如果是单个笔记或者是跟随文件夹的第一个笔记
            if (data.isSingle() || data.isOneFollowingFolder()) {
                // 设置为单个笔记的背景资源
                setBackgroundResource(NoteItemBgResources.getNoteBgSingleRes(id));
            // 如果是最后一个笔记
            } else if (data.isLast()) {
                // 设置为最后一个笔记的背景资源
                setBackgroundResource(NoteItemBgResources.getNoteBgLastRes(id));
            // 如果是第一个笔记或者是跟随多个文件夹的第一个笔记
            } else if (data.isFirst() || data.isMultiFollowingFolder()) {
                // 设置为第一个笔记的背景资源
                setBackgroundResource(NoteItemBgResources.getNoteBgFirstRes(id));
            } else {
                // 设置为普通笔记的背景资源
                setBackgroundResource(NoteItemBgResources.getNoteBgNormalRes(id));
            }
        } else {
            // 如果是文件夹类型，设置为文件夹的背景资源
            setBackgroundResource(NoteItemBgResources.getFolderBgRes());
        }
    }

    // 获取当前笔记项的数据
    public NoteItemData getItemData() {
        return mItemData;
    }
}
