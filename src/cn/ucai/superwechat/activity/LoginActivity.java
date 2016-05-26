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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Response;
import com.easemob.EMCallBack;

import cn.ucai.superwechat.I;
import cn.ucai.superwechat.R;
import cn.ucai.superwechat.applib.controller.HXSDKHelper;
import com.easemob.chat.EMChatManager;
import com.easemob.chat.EMGroupManager;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;

import cn.ucai.superwechat.Constant;
import cn.ucai.superwechat.SuperWeChatApplication;
import cn.ucai.superwechat.DemoHXSDKHelper;
import cn.ucai.superwechat.bean.Message;
import cn.ucai.superwechat.bean.User;
import cn.ucai.superwechat.data.ApiParams;
import cn.ucai.superwechat.data.GsonRequest;
import cn.ucai.superwechat.data.OkHttpUtils;
import cn.ucai.superwechat.db.EMUserDao;
import cn.ucai.superwechat.db.UserDao;
import cn.ucai.superwechat.listener.OnSetAvatarListener;
import cn.ucai.superwechat.task.DownloadAllGroupTask;
import cn.ucai.superwechat.task.DownloadContactListTask;
import cn.ucai.superwechat.task.DownloadPublicGroupTask;
import cn.ucai.superwechat.utils.CommonUtils;
import cn.ucai.superwechat.utils.MD5;
import cn.ucai.superwechat.utils.Utils;
//import cn.ucai.superwechat.domain.User;
/**
 * 登陆页面
 * 
 */
public class LoginActivity extends BaseActivity {
	private static final String TAG = "LoginActivity";
	public static final int REQUEST_CODE_SETNICK = 1;
	private EditText usernameEditText;
	private EditText passwordEditText;
	private Button mbtnUrl;

	private boolean progressShow;
	private boolean autoLogin = false;

	private String currentUsername;
	private String currentPassword;
	private Activity mContext;
	ProgressDialog pd;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// 如果用户名密码都有，直接进入主页面
		if (DemoHXSDKHelper.getInstance().isLogined()) {
			autoLogin = true;
			startActivity(new Intent(LoginActivity.this, MainActivity.class));
			return;
		}
		setContentView(R.layout.activity_login);
		mContext = this;

		usernameEditText = (EditText) findViewById(cn.ucai.superwechat.R.id.username);
		passwordEditText = (EditText) findViewById(cn.ucai.superwechat.R.id.password);
		mbtnUrl = (Button) findViewById(R.id.btnUrl);

		if (SuperWeChatApplication.getInstance().getUserName() != null) {
			usernameEditText.setText(SuperWeChatApplication.getInstance().getUserName());
		}

		setListener();
	}

	private void setListener() {
		login();
		onUserNameChangeListener();
		register();
	}

	private void onUserNameChangeListener() {
		// 如果用户名改变，清空密码
		usernameEditText.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				passwordEditText.setText(null);
			}
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {

			}
			@Override
			public void afterTextChanged(Editable s) {

			}
		});
	}

	/**
	 * 登录
	 */
	private void login() {
		findViewById(R.id.btnlogin).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!CommonUtils.isNetWorkConnected(mContext)) {
					Toast.makeText(mContext, cn.ucai.superwechat.R.string.network_isnot_available, Toast.LENGTH_SHORT).show();
					return;
				}
				currentUsername = usernameEditText.getText().toString().trim();
				currentPassword = passwordEditText.getText().toString().trim();

				if (TextUtils.isEmpty(currentUsername)) {
					Toast.makeText(mContext, cn.ucai.superwechat.R.string.User_name_cannot_be_empty, Toast.LENGTH_SHORT).show();
					return;
				}
				if (TextUtils.isEmpty(currentPassword)) {
					Toast.makeText(mContext, cn.ucai.superwechat.R.string.Password_cannot_be_empty, Toast.LENGTH_SHORT).show();
					return;
				}
				showProgressShow();
			//	final long start = System.currentTimeMillis();
				// 调用sdk登陆方法登陆聊天服务器
				EMChatManager.getInstance().login(currentUsername, currentPassword, new EMCallBack() {

					@Override
					public void onSuccess() {
						if (!progressShow) {
							return;
						}
						loginAppServer();
					}

					@Override
					public void onProgress(int progress, String status) {

					}

					@Override
					public void onError(final int code, final String message) {
						if (!progressShow) {
							return;
						}
						runOnUiThread(new Runnable() {
							public void run() {
								pd.dismiss();
								Toast.makeText(getApplicationContext(), getString(cn.ucai.superwechat.R.string.Login_failed) + message,
										Toast.LENGTH_SHORT).show();
							}
						});
					}
				});
			}
		});

	}
	//登录本地
	private void loginAppServer() {
		UserDao dao = new UserDao(mContext);
		cn.ucai.superwechat.bean.User user = dao.findUserByUserName(currentUsername);
		if (user!=null){
			if (user.getMUserPassword().equals(MD5.getData(currentPassword))){
			//	saveUser(user);
				loginSuccess();
			}else {
				pd.dismiss();
				Toast.makeText(getApplicationContext(), R.string.login_failure_failed, Toast.LENGTH_LONG).show();
			}
		}else{
			//http://10.0.2.2:8080/SuperWeChatServer/Server?
			// request=login&m_user_name=&m_user_password=
			try {
				String path = new ApiParams().with(I.User.USER_NAME,currentPassword)
                        .with(I.User.PASSWORD,currentPassword).getRequestUrl(I.REQUEST_LOGIN);
				executeRequest(new GsonRequest<User>(path,User.class,
						responseListener(),errorListener()));

			} catch (Exception e) {
				e.printStackTrace();
			}

		}

	}

	private Response.Listener<User> responseListener() {
		return new Response.Listener<User>(){
			@Override
			public void onResponse(User user) {
				if (user.isResult()){
					Log.i("main","-----------"+user.isResult()+"-----------");
					saveUser(user);
					user.setMUserPassword(MD5.getData(user.getMUserPassword()));
					UserDao dao = new UserDao(mContext);
					dao.addUser(user);

					loginSuccess();
				}else {
					pd.dismiss();
					Utils.showToast(mContext,Utils.getResourceString(mContext,user.getMsg()),Toast.LENGTH_LONG);
				}
			}
		};
	}

	private void saveUser(cn.ucai.superwechat.bean.User user) {
		Log.i("main","saveUser---"+user.toString()+"------------");
		SuperWeChatApplication instance = SuperWeChatApplication.getInstance();
		//保存用户信息
	//	UserDao userDao= new UserDao(mContext);
	//	userDao.addUser(user);

		instance.setUser(user);
		//登录成功，保存用户名，密码
		instance.setUserName(currentUsername);
		instance.setPassword(currentPassword);
		SuperWeChatApplication.currentUserNick = user.getMUserNick();
	}

	//登录成功
	private void loginSuccess(){
		// 登陆成功，保存用户名密码
	//	SuperWeChatApplication.getInstance().setUserName(currentUsername);
	//	SuperWeChatApplication.getInstance().setPassword(currentPassword);
		try {
			// ** 第一次登录或者之前logout后再登录，加载所有本地群和回话
			// ** manually load all local groups and
			EMGroupManager.getInstance().loadAllGroups();
			EMChatManager.getInstance().loadAllConversations();
			//下载用户头像保存到sd卡里面
			final OkHttpUtils<Message> utils = new OkHttpUtils<Message>();
			utils.url(SuperWeChatApplication.SERVER_ROOT)
					.addParam(I.KEY_REQUEST,I.REQUEST_DOWNLOAD_AVATAR)
					.addParam(I.AVATAR_TYPE,currentUsername)
					.doInBackground(new Callback() {
						@Override
						public void onFailure(Request request, IOException e) {
							Toast.makeText(mContext,e.getMessage(),Toast.LENGTH_LONG).show();
						}

						@Override
						public void onResponse(com.squareup.okhttp.Response response) throws IOException {
							String avatarPath = I.AVATAR_TYPE_USER_PATH + I.BACKSLASH
									+currentUsername + I.AVATAR_SUFFIX_JPG;
							File file = OnSetAvatarListener.getAvatarFile(mContext,avatarPath);
							FileOutputStream out = null;
							out = new FileOutputStream(file);
							utils.downloadFile(response,file,false);
						}
					}).execute(null);
			//登录成功后下载联系人，群组等资料
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Log.i("main","DownloadContactListTask   DownloadAllGroupTask    DownloadPublicGroupTask");
					new DownloadContactListTask(mContext,currentUsername).execute();
					new DownloadAllGroupTask(mContext,currentUsername).execute();
					new DownloadPublicGroupTask(mContext,currentUsername,I.PAGE_ID_DEFAULT,I.PAGE_SIZE_DEFAULT).execute();
				}
			});

			// 处理好友和群组
			initializeContacts();
		} catch (Exception e) {
			e.printStackTrace();
			// 取好友或者群聊失败，不让进入主页面
			runOnUiThread(new Runnable() {
				public void run() {
					pd.dismiss();
					DemoHXSDKHelper.getInstance().logout(true,null);
					Toast.makeText(getApplicationContext(), cn.ucai.superwechat.R.string.login_failure_failed, Toast.LENGTH_LONG).show();
				}
			});
			return;
		}
		// 更新当前用户的nickname 此方法的作用是在ios离线推送时能够显示用户nick
		boolean updatenick = EMChatManager.getInstance().updateCurrentUserNick(
				SuperWeChatApplication.currentUserNick.trim());
		if (!updatenick) {
			Log.e("LoginActivity", "update current user nick fail");
		}
		if (!LoginActivity.this.isFinishing() && pd.isShowing()) {
			pd.dismiss();
		}
		// 进入主页面
		Intent intent = new Intent(LoginActivity.this,
				MainActivity.class);
		startActivity(intent);

		finish();
	}

	private void showProgressShow() {
		progressShow = true;
		pd = new ProgressDialog(LoginActivity.this);
		pd.setCanceledOnTouchOutside(false);
		pd.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				progressShow = false;
			}
		});

		pd.setMessage(getString(R.string.Is_landing));
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				pd.show();
			}
		});

	}

	private void initializeContacts() {
		Map<String, cn.ucai.superwechat.domain.User> userlist = new HashMap<String, cn.ucai.superwechat.domain.User>();
		// 添加user"申请与通知"
		cn.ucai.superwechat.domain.User newFriends = new cn.ucai.superwechat.domain.User();
		newFriends.setUsername(Constant.NEW_FRIENDS_USERNAME);
		String strChat = getResources().getString(
				cn.ucai.superwechat.R.string.Application_and_notify);
		newFriends.setNick(strChat);

		userlist.put(Constant.NEW_FRIENDS_USERNAME, newFriends);
		// 添加"群聊"
		cn.ucai.superwechat.domain.User groupUser = new cn.ucai.superwechat.domain.User();
		String strGroup = getResources().getString(cn.ucai.superwechat.R.string.group_chat);
		groupUser.setUsername(Constant.GROUP_USERNAME);
		groupUser.setNick(strGroup);
		groupUser.setHeader("");
		userlist.put(Constant.GROUP_USERNAME, groupUser);
		
		// 添加"Robot"
		/*cn.ucai.superwechat.domain.User robotUser = new cn.ucai.superwechat.domain.User();
		String strRobot = getResources().getString(cn.ucai.superwechat.R.string.robot_chat);
		robotUser.setUsername(Constant.CHAT_ROBOT);
		robotUser.setNick(strRobot);
		robotUser.setHeader("");
		userlist.put(Constant.CHAT_ROBOT, robotUser);
		*/
		// 存入内存
		((DemoHXSDKHelper) HXSDKHelper.getInstance()).setContactList(userlist);
		// 存入db
		EMUserDao dao = new EMUserDao(LoginActivity.this);
		List<cn.ucai.superwechat.domain.User> users = new ArrayList<cn.ucai.superwechat.domain.User>(userlist.values());
		dao.saveContactList(users);
	}
	
	/**
	 * 注册
	 */
	private void register() {
		findViewById(R.id.btnregister).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivityForResult(new Intent(mContext, RegisterActivity.class), 0);
			}
		});

	}

	@Override
	protected void onResume() {
		super.onResume();
		if (autoLogin) {
			return;
		}
	}

}
