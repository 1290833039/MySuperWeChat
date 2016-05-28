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

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.toolbox.NetworkImageView;
import com.easemob.chat.EMGroup;
import com.easemob.chat.EMGroupManager;
import com.easemob.exceptions.EaseMobException;

import java.io.File;

import cn.ucai.superwechat.I;
import cn.ucai.superwechat.R;
import cn.ucai.superwechat.SuperWeChatApplication;
import cn.ucai.superwechat.bean.Contact;
import cn.ucai.superwechat.bean.Group;
import cn.ucai.superwechat.bean.Message;
import cn.ucai.superwechat.bean.User;
import cn.ucai.superwechat.data.ApiParams;
import cn.ucai.superwechat.data.GsonRequest;
import cn.ucai.superwechat.data.OkHttpUtils;
import cn.ucai.superwechat.listener.OnSetAvatarListener;
import cn.ucai.superwechat.utils.ImageUtils;
import cn.ucai.superwechat.utils.Utils;

public class NewGroupActivity extends BaseActivity {
	private EditText groupNameEditText;
	private ProgressDialog progressDialog;
	private EditText introductionEditText;
	private CheckBox checkBox;
	private CheckBox memberCheckbox;
	private LinearLayout openInviteContainer;

	NewGroupActivity mContext;
	OnSetAvatarListener mOnSetAvatarListener;
	ImageView ivAvatar;
	String avatarName;
	String st1;
	String st2;

	public static final int CREATE_NEW_GROUP = 100;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(cn.ucai.superwechat.R.layout.activity_new_group);
		mContext = this;
		initView();
		setListener();
	}

	private void setListener() {
		setOnCheckchangedListener();
		setSaveGroupClickListener();
		setGroupIconClickListener();
	}

	private void setGroupIconClickListener() {
		Log.i("main","NewGroupActivity---------> setGroupIconClickListener---------");
		findViewById(R.id.layout_group_icon).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.i("main","NewGroupActivity---------> setGroupIconClickListener---------1");
				mOnSetAvatarListener = new OnSetAvatarListener(mContext,R.id.layout_groups_avatar,
						getAvatarName(), I.AVATAR_TYPE_GROUP_PATH);
			}
		});
	}

	private void setSaveGroupClickListener() {
		Log.i("main","NewGroupActivity---------> setSaveGroupClickListener---------");
		findViewById(R.id.saveGroup).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String str6 = getResources().getString(cn.ucai.superwechat.R.string.Group_name_cannot_be_empty);
				String name = groupNameEditText.getText().toString();
				if (TextUtils.isEmpty(name)) {
					Log.i("main","NewGroupActivity---------> setSaveGroupClickListener---------1");
					Intent intent = new Intent(NewGroupActivity.this, AlertDialog.class);
					intent.putExtra("msg", str6);
					startActivity(intent);
				} else {
					Log.i("main","NewGroupActivity---------> setSaveGroupClickListener---------2");
					// 进通讯录选人
					startActivityForResult(new Intent(NewGroupActivity.this, GroupPickContactsActivity.class)
							.putExtra("groupName", name), CREATE_NEW_GROUP);
				}
			}
		});
	}

	private void setOnCheckchangedListener() {
		Log.i("main","NewGroupActivity---------> setOnCheckchangedListener---------1");
		checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked){
					openInviteContainer.setVisibility(View.INVISIBLE);
				}else{
					openInviteContainer.setVisibility(View.VISIBLE);
				}
			}
		});
	}

	private void initView() {
		groupNameEditText = (EditText) findViewById(cn.ucai.superwechat.R.id.edit_group_name);
		introductionEditText = (EditText) findViewById(cn.ucai.superwechat.R.id.edit_group_introduction);
		checkBox = (CheckBox) findViewById(cn.ucai.superwechat.R.id.cb_public);
		memberCheckbox = (CheckBox) findViewById(cn.ucai.superwechat.R.id.cb_member_inviter);
		openInviteContainer = (LinearLayout) findViewById(cn.ucai.superwechat.R.id.ll_open_invite);
		ivAvatar = (ImageView) findViewById(R.id.iv_GroupRavatar);
	}

	//添加头像文件名方法
	private String getAvatarName() {
		avatarName = System.currentTimeMillis()+"";
		return avatarName;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		st1 = getResources().getString(cn.ucai.superwechat.R.string.Is_to_create_a_group_chat);
		st2 = getResources().getString(cn.ucai.superwechat.R.string.Failed_to_create_groups);
		if (resultCode != RESULT_OK) {
			return;
		}
		if (requestCode == CREATE_NEW_GROUP) {
			setProgressDialog();
			//1、创建环信的群组，拿到hxid
			//2、创建远端的群组，并上传群组头像
			//3、添加群组成员到远端
			Log.i("main","NewGroupActivity---------> onActivityResult--------> newGroups");
			newGroups(data);
		}else{
			mOnSetAvatarListener.setAvatar(requestCode,data,ivAvatar);
		}
	}

	private void setProgressDialog() {
		progressDialog = new ProgressDialog(this);
		progressDialog.setMessage(st1);
		progressDialog.setCanceledOnTouchOutside(false);
		progressDialog.show();
	}

	//新建群组
	private void newGroups(final Intent data) {
		Log.i("main","NewGroupActivity---------> newGroups");
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				// 调用sdk创建群组方法
				String groupName = groupNameEditText.getText().toString().trim();
				String desc = introductionEditText.getText().toString();
				//自己自定义
				Contact[] contacts = (Contact[]) data.getSerializableExtra("newmembers");
				String[] members = null;
			//	String[] memberIds = null;
				EMGroup emGroup;

				if (contacts!=null){
					members = new String[contacts.length];
				//	memberIds = new String[contacts.length];
					for (int i=0;i<contacts.length;i++){
						members[i] = contacts[i].getMContactCname()+",";
					//	memberIds[i] = contacts[i].getMContactId()+",";
					}
				}
				Log.i("main","NewGroupActivity---------> newGroups---------->contacts.length= "+contacts.length);
				try {
					if(checkBox.isChecked()){
						//创建公开群，此种方式创建的群，可以自由加入
						//创建公开群，此种方式创建的群，用户需要申请，等群主同意后才能加入此群
						emGroup = EMGroupManager.getInstance().createPublicGroup(groupName, desc, members, true, 200);
					}else{
						//创建不公开群
						emGroup = EMGroupManager.getInstance().createPrivateGroup(groupName, desc, members, memberCheckbox.isChecked(),200);
					}
					//添加
					String hxId = emGroup.getGroupId();
					//创建本地群组
					Log.i("main","NewGroupActivity---------> createNewGroupAppServer");
					createNewGroupAppServer(hxId,groupName,desc,contacts);

					runOnUiThread(new Runnable() {
						public void run() {
							progressDialog.dismiss();
							setResult(RESULT_OK);
							finish();
						}
					});
				} catch (final EaseMobException e) {
					runOnUiThread(new Runnable() {
						public void run() {
							progressDialog.dismiss();
							Toast.makeText(NewGroupActivity.this, st2 + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
						}
					});
				}

			}
		});
	}

	private void createNewGroupAppServer(String hxId, String groupName, String desc, final Contact[] contacts) {
		User user = SuperWeChatApplication.getInstance().getUser();
		boolean isPublic = checkBox.isChecked();
		boolean isInvites = memberCheckbox.isChecked();
		//注册环信服务器 registerEmServer
		//先注册本地服务器并上传头像，REQUEST_REGISTER --> okhttp
		//添加群组成员
		File file  = new File(ImageUtils.getAvatarPath(mContext,I.AVATAR_TYPE_GROUP_PATH),
				avatarName+I.AVATAR_SUFFIX_JPG);

		Log.i("main","NewGroupActivity---------> createNewGroupAppServer------>path= "+file.getAbsolutePath());

		OkHttpUtils<Group> utils = new OkHttpUtils<Group>();
		utils.url(SuperWeChatApplication.SERVER_ROOT)//设置服务端根地址
				.addParam(I.KEY_REQUEST,I.REQUEST_CREATE_GROUP)//天津爱上传的请求参数
				.addParam(I.Group.HX_ID,hxId)//添加用户的账号
				.addParam(I.Group.NAME,groupName)//添加用户的昵称
				.addParam(I.Group.DESCRIPTION,desc)//添加用户密码
				.addParam(I.Group.OWNER,user.getMUserName())//添加用户密码
				.addParam(I.Group.IS_PUBLIC,isPublic+"")//添加用户密码
				.addParam(I.Group.ALLOW_INVITES,isInvites+"")//添加用户密码
				.addParam(I.User.USER_ID,user.getMUserId()+"")//添加用户密码
				.targetClass(Group.class)//设置服务端返回json数据的解析类型
				.addFile(file)//添加上传的文件
				.execute(new OkHttpUtils.OnCompleteListener<Group>() {
					@Override
					public void onSuccess(Group group) {
						if(group.isResult()){
							if (contacts!=null){
								//调用
								Log.i("main","NewGroupActivity---------> createNewGroupAppServer---------1");
								addGroupMembers(group,contacts);
							}else {
								Log.i("main","NewGroupActivity---------> createNewGroupAppServer----------2");
								SuperWeChatApplication.getInstance().getGroupList().add(group);
								Intent intent = new Intent("update_group_list").putExtra("group",group);
								setResult(RESULT_OK,intent);
								progressDialog.dismiss();
								Utils.showToast(mContext,Utils.getResourceString(mContext,R.string.Create_groups_Success),Toast.LENGTH_SHORT);
								finish();
							}

						}else {
							progressDialog.dismiss();
							Utils.showToast(mContext,Utils.getResourceString(mContext,group.getMsg()),Toast.LENGTH_SHORT);
							Log.i("main","NewGroupActivity---------> createNewGroupAppServer---"+Utils.getResourceString(mContext,group.getMsg()));
						}
					}

					@Override
					public void onError(String error) {
						Utils.showToast(mContext,error,Toast.LENGTH_SHORT);
					}
				});



	}
	//添加群组成员
	private void addGroupMembers(Group group, Contact[] contacts) {
		String userIds="";
		String userNames="";
		for (int i=0;i<contacts.length;i++){
			userIds+=contacts[i].getMContactCid()+",";
			userNames+=contacts[i].getMContactCname()+",";
		}
		try {
			String path = new ApiParams()
                    .with(I.Member.GROUP_HX_ID,group.getMGroupHxid())
                    .with(I.Member.USER_ID,userIds)
                    .with(I.Member.USER_NAME,userNames)
                    .getRequestUrl(I.REQUEST_ADD_GROUP_MEMBERS);
			Log.i("main","NewGroupActivity------addGroupMembers-----path="+path);
			executeRequest(new GsonRequest<Message>(path,Message.class,
					responseListener(group),errorListener()));

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private Response.Listener<Message> responseListener(final Group group) {
		return new Response.Listener<Message>() {
			@Override
			public void onResponse(Message message) {
				if (message.isResult()){
					Log.i("main","NewGroupActivity------addGroupMembers-----OK");
					progressDialog.dismiss();
					SuperWeChatApplication.getInstance().getGroupList().add(group);
					Intent intent = new Intent("update_group_list").putExtra("group",group);
					Utils.showToast(mContext,Utils.getResourceString(mContext,group.getMsg()),Toast.LENGTH_LONG);
					setResult(RESULT_OK,intent);
				}else{
					Log.i("main","NewGroupActivity------addGroupMembers-----Error");
					progressDialog.dismiss();
					Utils.showToast(mContext,R.string.Failed_to_create_groups,Toast.LENGTH_LONG);
				}
				finish();
			}
		};
	}

	public void back(View view) {
		finish();
	}
}
