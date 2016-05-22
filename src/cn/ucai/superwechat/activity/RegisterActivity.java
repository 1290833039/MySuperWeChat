/**
 * Copyright (C) 2013-2014 EaseMob Technologies. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.ucai.superwechat.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.easemob.EMError;
import com.easemob.chat.EMChatManager;

import cn.ucai.superwechat.I;
import cn.ucai.superwechat.R;
import cn.ucai.superwechat.SuperWeChatApplication;
import cn.ucai.superwechat.bean.Message;
import cn.ucai.superwechat.data.OkHttpUtils;
import cn.ucai.superwechat.listener.OnSetAvatarListener;
import cn.ucai.superwechat.utils.ImageUtils;
import cn.ucai.superwechat.utils.Utils;

import com.easemob.exceptions.EaseMobException;

import java.io.File;

/**
 * 注册页
 * 
 */
public class RegisterActivity extends BaseActivity {
	private Activity mContext;
	private EditText userNameEditText;
	private EditText nickNameEditText;
	private EditText passwordEditText;
	private EditText confirmPwdEditText;
	private ImageView mIVAvatar;
	private String avatarName;
	ProgressDialog pd;
	String username;
	String pwd;
	String nick;

	OnSetAvatarListener mOnSetAvatarListener;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(cn.ucai.superwechat.R.layout.activity_register);
		mContext = this;
		initView();
		setListener();
	}

	private void setListener() {
		register();
		backLogin();
		onSetAvatarListener();
	}

	private void onSetAvatarListener() {
		findViewById(R.id.layout_user_avatar).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mOnSetAvatarListener = new OnSetAvatarListener(mContext,R.id.layout_register,getAvatarName(), I.AVATAR_TYPE_USER_PATH);
			}
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode==RESULT_OK){
			mOnSetAvatarListener.setAvatar(requestCode,data,mIVAvatar);
		}

	}

	private String getAvatarName() {
		avatarName = System.currentTimeMillis()+"";
		return avatarName;
	}

	private void backLogin() {
		findViewById(R.id.btnLogin).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}

	private void initView() {
		userNameEditText = (EditText) findViewById(R.id.username);
		passwordEditText = (EditText) findViewById(R.id.password);
		confirmPwdEditText = (EditText) findViewById(R.id.confirm_password);
		nickNameEditText = (EditText) findViewById(R.id.NickName);
		mIVAvatar = (ImageView) findViewById(R.id.iv_Ravatar);
	}

	/**
	 * 注册
	 */
	private void register() {
		findViewById(R.id.btnRegister).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				username = userNameEditText.getText().toString().trim();
				pwd = passwordEditText.getText().toString().trim();
				nick = nickNameEditText.getText().toString().trim();

				String confirm_pwd = confirmPwdEditText.getText().toString().trim();
				if (TextUtils.isEmpty(username)) {
					userNameEditText.requestFocus();
					userNameEditText.setError(getResources().getString(R.string.User_name_cannot_be_empty));
					return;
				} else if(username.matches("[\\w][\\w\\d_]+")){//正则表达式：第一个字符必须为字母，之后可以为字母，数字，_
					userNameEditText.requestFocus();
					userNameEditText.setError(getResources().getString(R.string.User_name_cannot_be_wd));
					return;
				} else if (TextUtils.isEmpty(nick)) {
					nickNameEditText.requestFocus();
					nickNameEditText.setError(getResources().getString(R.string.Nick_name_cannot_be_empty));
					return;
				} else if (TextUtils.isEmpty(pwd)) {
					passwordEditText.requestFocus();
					passwordEditText.setError(getResources().getString(R.string.Password_cannot_be_empty));
					return;
				} else if (TextUtils.isEmpty(confirm_pwd)) {
					confirmPwdEditText.requestFocus();
					confirmPwdEditText.setError(getResources().getString(R.string.Confirm_password_cannot_be_empty));
					return;
				} else if (!pwd.equals(confirm_pwd)) {
					confirmPwdEditText.setError(getResources().getString(R.string.Two_input_password));
					return;
				}

				if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(pwd)) {
					pd = new ProgressDialog(mContext);
					pd.setMessage(getResources().getString(cn.ucai.superwechat.R.string.Is_the_registered));
					pd.show();

					registerAppServer();

				}
			}
		});
	}

	private void registerAppServer() {
		//首先注册远端服务器，并上传头像    ---okHttp
		//注册环信账号
		//如果环信注册失败，则调用取消注册方法，删除远端服务器账号和图片
		//url= http://10.0.2.2:8080/SuperWeChatServer/Server?
		// request=register&m_user_name=&m_user_nick=&m_user_password=

		File file = new File(ImageUtils.getAvatarPath(mContext,I.AVATAR_TYPE_USER_PATH),
				username + I.AVATAR_SUFFIX_JPG);
		final OkHttpUtils<Message> utils = new OkHttpUtils<Message>();
		utils.url(SuperWeChatApplication.SERVER_ROOT)
				.addParam(I.KEY_REQUEST,I.REQUEST_REGISTER)
				.addParam(I.User.USER_NAME,username)
				.addParam(I.User.NICK,nick)
				.addParam(I.User.PASSWORD,pwd)
				.targetClass(Message.class)
				.addFile(file)
				.execute(new OkHttpUtils.OnCompleteListener<Message>(){
					@Override
					public void onSuccess(Message result) {
						if (result.isResult()){
							registerEMServer();
						}else {
							pd.dismiss();
							Utils.showToast(mContext,Utils.getResourceString(mContext,result.getMsg()),Toast.LENGTH_LONG);

						}
					}

					@Override
					public void onError(String error) {
						pd.dismiss();
						Utils.showToast(mContext,error,Toast.LENGTH_LONG);

					}
				});

	}

	/** 注册环信账号 */
	private void registerEMServer(){
		new Thread(new Runnable() {
			public void run() {
				try {
					// 调用sdk注册方法
					EMChatManager.getInstance().createAccountOnServer(username, pwd);
					runOnUiThread(new Runnable() {
						public void run() {
							if (!RegisterActivity.this.isFinishing())
								pd.dismiss();
							// 保存用户名
							SuperWeChatApplication.getInstance().setUserName(username);
							Toast.makeText(getApplicationContext(), getResources().getString(cn.ucai.superwechat.R.string.Registered_successfully), Toast.LENGTH_LONG).show();
							finish();
						}
					});
				} catch (final EaseMobException e) {
					//调用取消注册方法
					unRegister();

					runOnUiThread(new Runnable() {
						public void run() {
							if (!RegisterActivity.this.isFinishing())
								pd.dismiss();
							int errorCode=e.getErrorCode();
							if(errorCode==EMError.NONETWORK_ERROR){
								Toast.makeText(getApplicationContext(), getResources().getString(cn.ucai.superwechat.R.string.network_anomalies), Toast.LENGTH_SHORT).show();
							}else if(errorCode == EMError.USER_ALREADY_EXISTS){
								Toast.makeText(getApplicationContext(), getResources().getString(cn.ucai.superwechat.R.string.User_already_exists), Toast.LENGTH_SHORT).show();
							}else if(errorCode == EMError.UNAUTHORIZED){
								Toast.makeText(getApplicationContext(), getResources().getString(cn.ucai.superwechat.R.string.registration_failed_without_permission), Toast.LENGTH_SHORT).show();
							}else if(errorCode == EMError.ILLEGAL_USER_NAME){
								Toast.makeText(getApplicationContext(), getResources().getString(cn.ucai.superwechat.R.string.illegal_user_name),Toast.LENGTH_SHORT).show();
							}else{
								Toast.makeText(getApplicationContext(), getResources().getString(cn.ucai.superwechat.R.string.Registration_failed) + e.getMessage(), Toast.LENGTH_SHORT).show();
							}
						}
					});
				}
			}
		}).start();
	}
	//取消注册方法
	private void unRegister() {
		//url=http://10.0.2.2:8080/SuperWeChatServer/Server?
		// request=unregister&m_user_name=
		OkHttpUtils<Message> utils = new OkHttpUtils<Message>();
		utils.url(SuperWeChatApplication.SERVER_ROOT)
				.addParam(I.KEY_REQUEST,I.REQUEST_UNREGISTER)
				.addParam(I.User.USER_NAME,username)
				.targetClass(Message.class)
				.execute(new OkHttpUtils.OnCompleteListener<Message>(){
					@Override
					public void onSuccess(Message result) {
						pd.dismiss();
						Utils.showToast(mContext,Utils.getResourceString(mContext,result.getMsg()),Toast.LENGTH_LONG);

					}
					@Override
					public void onError(String error) {
						pd.dismiss();
						Utils.showToast(mContext,error,Toast.LENGTH_LONG);

					}
				});
	}

	public void back(View view) {
		finish();
	}

}
