/**
 * Copyright (C) 2013-2014 EaseMob Technologies. All rights reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.ucai.superwechat.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import cn.ucai.superwechat.I;
import cn.ucai.superwechat.R;
import cn.ucai.superwechat.applib.controller.HXSDKHelper;

import com.android.volley.Response;
import com.android.volley.toolbox.NetworkImageView;
import com.easemob.chat.EMContactManager;

import java.util.HashMap;

import cn.ucai.superwechat.SuperWeChatApplication;
import cn.ucai.superwechat.DemoHXSDKHelper;
import cn.ucai.superwechat.bean.Contact;
import cn.ucai.superwechat.bean.Message;
import cn.ucai.superwechat.bean.User;
import cn.ucai.superwechat.data.ApiParams;
import cn.ucai.superwechat.data.GsonRequest;
import cn.ucai.superwechat.data.OkHttpUtils;
import cn.ucai.superwechat.utils.UserUtils;
import cn.ucai.superwechat.utils.Utils;

public class AddContactActivity extends BaseActivity {
    private EditText editText;
    private LinearLayout searchedUserLayout;
    private TextView nameText, mTextView;
    private Button searchBtn;
    private TextView mNotFind;
    private NetworkImageView avatar;
    private InputMethodManager inputMethodManager;
    private String toAddUsername;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_contact);
        //头部 title
        mTextView = (TextView) findViewById(R.id.add_list_friends);
        String strAdd = getResources().getString(R.string.add_friend);
        mTextView.setText(strAdd);
        //用户名
        editText = (EditText) findViewById(R.id.edit_note);
        String strUserName = getResources().getString(R.string.user_name);
        editText.setHint(strUserName);
        //显示搜索结果的布局
        searchedUserLayout = (LinearLayout) findViewById(cn.ucai.superwechat.R.id.ll_user);
        avatar = (NetworkImageView) findViewById(cn.ucai.superwechat.R.id.avatar);
        nameText = (TextView) findViewById(cn.ucai.superwechat.R.id.name);
        searchBtn = (Button) findViewById(cn.ucai.superwechat.R.id.search);
        inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        mNotFind = (TextView) findViewById(R.id.notFind);
    }


    /**
     * 查找按钮监听事件
     * 查找contact
     * @param v
     */
    public void searchContact(View v) {
        //用户名
        String name = editText.getText().toString();
        String saveText = searchBtn.getText().toString();
        String userName = SuperWeChatApplication.getInstance().getUserName();

        Log.i("main",userName+"----------"+name);

        if (TextUtils.isEmpty(name)) {
            String st = getResources().getString(R.string.Please_enter_a_username);
            startActivity(new Intent(this, AlertDialog.class).putExtra("msg", st));
            return;
        }
        //不能添加自己
        if (SuperWeChatApplication.getInstance().getUserName().equals(name.trim())) {
            searchedUserLayout.setVisibility(View.GONE);
            mNotFind.setVisibility(View.GONE);
            String str = getString(R.string.not_add_myself);
            startActivity(new Intent(this, AlertDialog.class).putExtra("msg", str));
            return;
        }
        toAddUsername = name;

        // TODO 从服务器获取此contact,如果不存在提示不存在此用户
        //http://10.0.2.2:8080/SuperWeChatServer/Server?request=find_user&m_user_name=
        try {
            String path = new ApiParams().with(I.User.USER_NAME,toAddUsername)
                    .getRequestUrl(I.REQUEST_FIND_USER);
            executeRequest(new GsonRequest<User>(path,User.class,
                    responseListener(),errorListener()));

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private Response.Listener<User> responseListener() {
        return new Response.Listener<User>() {
            @Override
            public void onResponse(User user) {
                if (user!=null){
                    //判断是否为自己的好友
                    HashMap<String, Contact> userList = SuperWeChatApplication.getInstance().getUserList();
                    if (userList.containsKey(user.getMUserName())){
                        startActivity(new Intent(AddContactActivity.this,UserProfileActivity.class)
                        .putExtra("username",user.getMUserName()));
                    }else{
                        searchedUserLayout.setVisibility(View.VISIBLE);
                        //设置用户头像,昵称
                        UserUtils.setUserBeanAvatar(user,avatar);
                        UserUtils.setUserBeanNick(user,nameText);
                    }

                    mNotFind.setVisibility(View.GONE);

                }else{
                    //服务器存在此用户，显示此用户和添加按钮
                    searchedUserLayout.setVisibility(View.GONE);
                    mNotFind.setVisibility(View.VISIBLE);
                }
            }
        };
    }

    /**
     *  添加按钮监听事件
     *  添加contact
     * @param view
     */
    public void addContact(View view) {
        if (((DemoHXSDKHelper) HXSDKHelper.getInstance()).getContactList().containsKey(nameText.getText().toString())) {
            //提示已在好友列表中，无需添加
            if (EMContactManager.getInstance().getBlackListUsernames().contains(nameText.getText().toString())) {
                startActivity(new Intent(this, AlertDialog.class).putExtra("msg", "此用户已是你好友(被拉黑状态)，从黑名单列表中移出即可"));
                return;
            }
            String strin = getString(cn.ucai.superwechat.R.string.This_user_is_already_your_friend);
            startActivity(new Intent(this, AlertDialog.class).putExtra("msg", strin));
            return;
        }

        progressDialog = new ProgressDialog(this);
        String stri = getResources().getString(R.string.Is_sending_a_request);
        progressDialog.setMessage(stri);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();

        new Thread(new Runnable() {
            public void run() {

                try {
                    //demo写死了个reason，实际应该让用户手动填入
                    String s = getResources().getString(cn.ucai.superwechat.R.string.Add_a_friend);
                    EMContactManager.getInstance().addContact(toAddUsername, s);
                    runOnUiThread(new Runnable() {
                        public void run() {
                            progressDialog.dismiss();
                            String s1 = getResources().getString(cn.ucai.superwechat.R.string.send_successful);
                            Toast.makeText(getApplicationContext(), s1, Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            progressDialog.dismiss();
                            String s2 = getResources().getString(cn.ucai.superwechat.R.string.Request_add_buddy_failure);
                            Toast.makeText(getApplicationContext(), s2 + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }

    public void back(View v) {
        finish();
    }
}
