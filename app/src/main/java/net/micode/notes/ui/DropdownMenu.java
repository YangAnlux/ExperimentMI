/*
 * Copyright (c) 2010-2011, The MiCode Open Source Community (www.micode.net)
 *
 * Licensed under the Apache License...（保留原有版权信息）
 */

package net.micode.notes.ui;

// 模块/库导入注释 ======================================================
import android.content.Context;            // 上下文环境
import android.view.Menu;                 // 菜单操作接口
import android.view.MenuItem;             // 菜单项操作接口
import android.view.View;                 // 视图基类
import android.view.View.OnClickListener; // 点击监听接口
import android.widget.Button;             // 按钮控件
import android.widget.PopupMenu;          // 弹出式菜单控件
import android.widget.PopupMenu.OnMenuItemClickListener; // 菜单项点击监听

import net.micode.notes.R;                // 资源文件

// 类注释 =============================================================
/**
 * 下拉菜单封装类
 * 功能：将按钮与弹出菜单结合，实现下拉选择功能
 * 特性：
 * 1. 通过按钮触发弹出菜单
 * 2. 支持自定义菜单资源
 * 3. 提供菜单项点击回调
 * 4. 动态修改按钮标题
 * 
 * 使用场景：需要在下拉选择时展示多个操作选项
 */
public class DropdownMenu {
    // 成员变量注释 =====================================================
    private Button mButton;        // 触发菜单的按钮控件
    private PopupMenu mPopupMenu;  // 弹出菜单实例
    private Menu mMenu;            // 菜单内容对象

    // 构造方法 ========================================================
    /**
     * 初始化下拉菜单
     * @param context 上下文环境
     * @param button 绑定的按钮控件
     * @param menuId 菜单资源ID（R.menu.xxx）
     *
     * >>> 初始化流程：
     * 1. 设置按钮下拉图标
     * 2. 创建弹出菜单并绑定到按钮
     * 3. 加载菜单资源
     * 4. 设置按钮点击事件
     */
    public DropdownMenu(Context context, Button button, int menuId) {
        mButton = button;
        // 设置按钮右侧下拉指示图标
        mButton.setBackgroundResource(R.drawable.dropdown_icon);
        
        // 创建弹出菜单（锚点为按钮）
        mPopupMenu = new PopupMenu(context, mButton);
        mMenu = mPopupMenu.getMenu();
        
        // 加载菜单布局
        mPopupMenu.getMenuInflater().inflate(menuId, mMenu);
        
        // 绑定按钮点击事件
        mButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mPopupMenu.show(); // 显示弹出菜单
            }
        });
    }

    // 公开方法 ========================================================
    /**
     * 设置菜单项点击监听器
     * @param listener 菜单项点击回调接口
     * 
     * 最佳实践：建议在Activity/Fragment中实现OnMenuItemClickListener接口
     */
    public void setOnDropdownMenuItemClickListener(OnMenuItemClickListener listener) {
        if (mPopupMenu != null) {
            mPopupMenu.setOnMenuItemClickListener(listener);
        }
    }

    /**
     * 查找菜单项
     * @param id 菜单项资源ID（R.id.xxx）
     * @return 对应的MenuItem对象（可能为null）
     * 
     * 边界条件：当菜单项不存在时返回null，调用方需做空判断
     */
    public MenuItem findItem(int id) {
        return mMenu.findItem(id);
    }

    /**
     * 设置按钮显示文本
     * @param title 要显示的标题文本
     * 
     * 注意：此方法修改的是按钮文本，不是菜单标题
     */
    public void setTitle(CharSequence title) {
        mButton.setText(title);
    }
}
