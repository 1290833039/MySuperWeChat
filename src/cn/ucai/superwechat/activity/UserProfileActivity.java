package cn.ucai.superwechat.activity;

import java.io.ByteArrayOutputStream;
import java.io.File;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.toolbox.NetworkImageView;
import com.easemob.EMValueCallBack;

import cn.ucai.superwechat.I;
import cn.ucai.superwechat.R;
import cn.ucai.superwechat.SuperWeChatApplication;
import cn.ucai.superwechat.applib.controller.HXSDKHelper;
import com.easemob.chat.EMChatManager;
import cn.ucai.superwechat.DemoHXSDKHelper;
import cn.ucai.superwechat.bean.Message;
import cn.ucai.superwechat.data.ApiParams;
import cn.ucai.superwechat.data.GsonRequest;
import cn.ucai.superwechat.data.MultipartRequest;
import cn.ucai.superwechat.data.RequestManager;
import cn.ucai.superwechat.db.UserDao;
import cn.ucai.superwechat.domain.User;
import cn.ucai.superwechat.listener.OnSetAvatarListener;
import cn.ucai.superwechat.utils.ImageUtils;
import cn.ucai.superwechat.utils.UserUtils;
import cn.ucai.superwechat.utils.Utils;

import com.squareup.picasso.Picasso;

public class UserProfileActivity extends BaseActivity implements OnClickListener{
	
	private static final int REQUESTCODE_PICK = 1;
	private static final int REQUESTCODE_CUTTING = 2;
	private NetworkImageView headAvatar;
	private ImageView headPhotoUpdate;
	private ImageView iconRightArrow;
	private TextView tvNickName;
	private TextView tvUsername;
	private ProgressDialog dialog;
	private RelativeLayout rlNickName;

	UserProfileActivity mContext;
	OnSetAvatarListener mOnSetAvatarListener;
	String avatarName;

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		setContentView(cn.ucai.superwechat.R.layout.activity_user_profile);
		mContext = this;
		initView();
		initListener();
	}
	
	private void initView() {
		headAvatar = (NetworkImageView) findViewById(cn.ucai.superwechat.R.id.user_head_avatar);
		headPhotoUpdate = (ImageView) findViewById(cn.ucai.superwechat.R.id.user_head_headphoto_update);
		tvUsername = (TextView) findViewById(cn.ucai.superwechat.R.id.user_username);
		tvNickName = (TextView) findViewById(cn.ucai.superwechat.R.id.user_nickname);
		rlNickName = (RelativeLayout) findViewById(cn.ucai.superwechat.R.id.rl_nickname);
		iconRightArrow = (ImageView) findViewById(cn.ucai.superwechat.R.id.ic_right_arrow);
	}
	
	private void initListener() {
		Intent intent = getIntent();
		String username = intent.getStringExtra("username");
		boolean enableUpdate = intent.getBooleanExtra("setting", false);
		if (enableUpdate) {
			headPhotoUpdate.setVisibility(View.VISIBLE);
			iconRightArrow.setVisibility(View.VISIBLE);
			rlNickName.setOnClickListener(this);
			headAvatar.setOnClickListener(this);
		} else {
			headPhotoUpdate.setVisibility(View.GONE);
			iconRightArrow.setVisibility(View.INVISIBLE);
		}//修改后的用户信息
		if (username == null || username.equals(SuperWeChatApplication.getInstance().getUserName())) {
			tvUsername.setText(SuperWeChatApplication.getInstance().getUserName());
			UserUtils.setCurrentUserBeanNick(tvNickName);
			UserUtils.setCurrentUserAvatar(headAvatar);
		}  else {
			tvUsername.setText(username);
			UserUtils.setUserBeanNick(username, tvNickName);
			UserUtils.setUserBeanAvatar(username, headAvatar);
		//	asyncFetchUserInfo(username);
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case cn.ucai.superwechat.R.id.user_head_avatar:
			//实例化OnSetAvatarListener对象
			mOnSetAvatarListener = new OnSetAvatarListener(mContext,R.id.layout_user_profile,
					getAvatarName(),I.AVATAR_TYPE_USER_PATH);
			Log.i("main","onClick--------------> onActivityResult");
		//	uploadHeadPhoto();
			break;
		case cn.ucai.superwechat.R.id.rl_nickname:
			final EditText editText = new EditText(this);
			new AlertDialog.Builder(this).setTitle(cn.ucai.superwechat.R.string.setting_nickname).setIcon(android.R.drawable.ic_dialog_info).setView(editText)
					.setPositiveButton(cn.ucai.superwechat.R.string.dl_ok, new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							String nickString = editText.getText().toString();
							if (TextUtils.isEmpty(nickString)) {
								Toast.makeText(UserProfileActivity.this, getString(cn.ucai.superwechat.R.string.toast_nick_not_isnull), Toast.LENGTH_SHORT).show();
								return;
							}
							//先修改远端
							updateUserNick(nickString);
						}
					}).setNegativeButton(cn.ucai.superwechat.R.string.dl_cancel, null).show();
			break;
		default:
			break;
		}

	}
	//添加头像文件名方法
	private String getAvatarName() {
		avatarName = System.currentTimeMillis()+"";
		return avatarName;
	}

	//更新远端服务器用户昵称
	private void updateUserNick(String nickName){
		try {
			String path = new ApiParams()
                    .with(I.User.USER_NAME,SuperWeChatApplication.getInstance().getUserName())
                    .with(I.User.NICK,nickName)
                    .getRequestUrl(I.REQUEST_UPDATE_USER_NICK);
			Log.i("main","updateUserNick--------> path= "+path);
			executeRequest(new GsonRequest<cn.ucai.superwechat.bean.User>(path,cn.ucai.superwechat.bean.User.class,
					responseUpdateNickListener(nickName),errorListener()));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private Response.Listener<cn.ucai.superwechat.bean.User> responseUpdateNickListener(final String nickName) {
		return new Response.Listener<cn.ucai.superwechat.bean.User>() {
			@Override
			public void onResponse(cn.ucai.superwechat.bean.User user) {
				if (user!=null && user.isResult()){
					Log.i("main","responseUpdateNickListener--------> Ok--");
					//修改环信
					updateRemoteNick(nickName);
				}else{
					Utils.showToast(mContext,Utils.getResourceString(mContext,user.getMsg()),Toast.LENGTH_LONG);
					dialog.dismiss();
					Log.i("main","responseUpdateNickListener--------> Error--");
				}
			}
		};

	}

	public void asyncFetchUserInfo(String username){
		((DemoHXSDKHelper)HXSDKHelper.getInstance()).getUserProfileManager().asyncGetUserInfo(username, new EMValueCallBack<User>() {
			
			@Override
			public void onSuccess(User user) {
				if (user != null) {
					tvNickName.setText(user.getNick());
					if(!TextUtils.isEmpty(user.getAvatar())){
						 Picasso.with(UserProfileActivity.this).load(user.getAvatar()).placeholder(cn.ucai.superwechat.R.drawable.default_avatar).into(headAvatar);
					}else{
						Picasso.with(UserProfileActivity.this).load(cn.ucai.superwechat.R.drawable.default_avatar).into(headAvatar);
					}
					UserUtils.saveUserInfo(user);
				}
			}
			
			@Override
			public void onError(int error, String errorMsg) {
			}
		});
	}
	
	
	
	private void uploadHeadPhoto() {
		AlertDialog.Builder builder = new Builder(this);
		builder.setTitle(cn.ucai.superwechat.R.string.dl_title_upload_photo);
		builder.setItems(new String[] { getString(cn.ucai.superwechat.R.string.dl_msg_take_photo), getString(cn.ucai.superwechat.R.string.dl_msg_local_upload) },
				new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						switch (which) {
						case 0:
							Toast.makeText(UserProfileActivity.this, getString(cn.ucai.superwechat.R.string.toast_no_support),
									Toast.LENGTH_SHORT).show();
							break;
						case 1:
							Intent pickIntent = new Intent(Intent.ACTION_PICK,null);
							pickIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
							startActivityForResult(pickIntent, REQUESTCODE_PICK);
							break;
						default:
							break;
						}
					}
				});
		builder.create().show();
	}
	
	

	private void updateRemoteNick(final String nickName) {
		dialog = ProgressDialog.show(this, getString(cn.ucai.superwechat.R.string.dl_update_nick), getString(cn.ucai.superwechat.R.string.dl_waiting));
		new Thread(new Runnable() {
			@Override
			public void run() {
				boolean updatenick = ((DemoHXSDKHelper)HXSDKHelper.getInstance()).getUserProfileManager().updateParseNickName(nickName);
				if (UserProfileActivity.this.isFinishing()) {
					return;
				}
				if (!updatenick) {
					runOnUiThread(new Runnable() {
						public void run() {
							Toast.makeText(UserProfileActivity.this, getString(cn.ucai.superwechat.R.string.toast_updatenick_fail), Toast.LENGTH_SHORT)
									.show();
							dialog.dismiss();
						}
					});
				} else {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							dialog.dismiss();
							Toast.makeText(UserProfileActivity.this, getString(cn.ucai.superwechat.R.string.toast_updatenick_success), Toast.LENGTH_SHORT)
									.show();
							tvNickName.setText(nickName);
							//更新环信端成功后去更新本地数据库
							SuperWeChatApplication.currentUserNick = nickName;
							cn.ucai.superwechat.bean.User user = SuperWeChatApplication.getInstance().getUser();
							user.setMUserNick(nickName);
							UserDao dao = new UserDao(mContext);
							dao.updateUser(user);

							Log.i("main","updateRemoteNick--------> 更新远端服务用户昵称--");
						}
					});
				}
			}
		}).start();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		/*switch (requestCode) {
		case REQUESTCODE_PICK:
			if (data == null || data.getData() == null) {
				return;
			}
			startPhotoZoom(data.getData());
			break;
		case REQUESTCODE_CUTTING:
			if (data != null) {
				setPicToView(data);
			}
			break;
		default:
			break;
		}*/
		Log.i("main","onActivityResult--------------> ");
		super.onActivityResult(requestCode, resultCode, data);
		//添加OnSetAvatarListener返回结果判定
		mOnSetAvatarListener.setAvatar(requestCode,data,headAvatar);
		if (resultCode==RESULT_OK && requestCode==OnSetAvatarListener.REQUEST_CROP_PHOTO){
			//删除cach的图片
			dialog = ProgressDialog.show(this, getString(cn.ucai.superwechat.R.string.dl_update_photo), getString(cn.ucai.superwechat.R.string.dl_waiting));
			RequestManager.getRequestQueue().getCache()
					.remove(UserUtils.getAvatarPath(SuperWeChatApplication.getInstance().getUserName()));

			Log.i("main","onActivityResult--------> updateUserAvatarByMultipart--");
			//上传头像方法
			updateUserAvatarByMultipart();
			dialog.show();
		}
	}
	private final String boundary = "apiclient-" + System.currentTimeMillis();
	private final String mimeType = "multipart/form-data;boundary=" + boundary;
	private byte[] multipartBody;
	private Bitmap bitmap;

	//上传头像
	private void updateUserAvatarByMultipart() {
		File file = new File(ImageUtils.getAvatarPath(mContext, I.AVATAR_TYPE_USER_PATH),
				avatarName + I.AVATAR_SUFFIX_JPG);

		String path = file.getAbsolutePath();
		bitmap = BitmapFactory.decodeFile(path);
		multipartBody = getImageBytes(bitmap);
		String url = null;
		Log.i("main","updateUserAvatarByMultipart-----path= "+path);
		try {
			//更新头像url
			url = new ApiParams()
                    .with(I.User.USER_NAME,SuperWeChatApplication.getInstance().getUserName())
					.with(I.AVATAR_TYPE,I.AVATAR_TYPE_USER_PATH)
                    .getRequestUrl(I.REQUEST_UPLOAD_AVATAR);

			Log.i("main","updateUserAvatarByMultipart-----url= "+url);
			executeRequest(new MultipartRequest<Message>(url,Message.class,null,
					responseUpdateAvatarListener(),errorListener(),mimeType,multipartBody));
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private Response.Listener<Message> responseUpdateAvatarListener() {
		return new Response.Listener<Message>() {
			@Override
			public void onResponse(Message result) {
				if (result.isResult()){
					//成功
					UserUtils.setCurrentUserAvatar(headAvatar);
				//	Utils.showToast(mContext,Utils.getResourceString(mContext,result.getMsg()),Toast.LENGTH_LONG);
					dialog.dismiss();
					Log.i("main","responseUpdateAvatarListener------- OK");
				}else{
					//失败
					Toast.makeText(UserProfileActivity.this, getString(cn.ucai.superwechat.R.string.toast_updatephoto_fail),
							Toast.LENGTH_SHORT).show();
					dialog.dismiss();
					UserUtils.setCurrentUserAvatar(headAvatar);
					Log.i("main","responseUpdateAvatarListener------- Error");
				}
			}
		};
	}

	public byte[] getImageBytes(Bitmap bmp){
		if(bmp==null)return null;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		bmp.compress(Bitmap.CompressFormat.JPEG,100,baos);
		byte[] imageBytes = baos.toByteArray();
		return imageBytes;
	}

	public void startPhotoZoom(Uri uri) {
		Intent intent = new Intent("com.android.camera.action.CROP");
		intent.setDataAndType(uri, "image/*");
		intent.putExtra("crop", true);
		intent.putExtra("aspectX", 1);
		intent.putExtra("aspectY", 1);
		intent.putExtra("outputX", 300);
		intent.putExtra("outputY", 300);
		intent.putExtra("return-data", true);
		intent.putExtra("noFaceDetection", true);
		startActivityForResult(intent, REQUESTCODE_CUTTING);
	}
	
	/**
	 * save the picture data
	 * 
	 * @param picdata
	 */
	private void setPicToView(Intent picdata) {
		Bundle extras = picdata.getExtras();
		if (extras != null) {
			Bitmap photo = extras.getParcelable("data");
			Drawable drawable = new BitmapDrawable(getResources(), photo);
			headAvatar.setImageDrawable(drawable);
			uploadUserAvatar(Bitmap2Bytes(photo));
		}

	}
	
	private void uploadUserAvatar(final byte[] data) {
		dialog = ProgressDialog.show(this, getString(cn.ucai.superwechat.R.string.dl_update_photo), getString(cn.ucai.superwechat.R.string.dl_waiting));
		new Thread(new Runnable() {
			@Override
			public void run() {
				final String avatarUrl = ((DemoHXSDKHelper)HXSDKHelper.getInstance()).getUserProfileManager().uploadUserAvatar(data);
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						dialog.dismiss();
						if (avatarUrl != null) {
							Toast.makeText(UserProfileActivity.this, getString(cn.ucai.superwechat.R.string.toast_updatephoto_success),
									Toast.LENGTH_SHORT).show();
						} else {
							Toast.makeText(UserProfileActivity.this, getString(cn.ucai.superwechat.R.string.toast_updatephoto_fail),
									Toast.LENGTH_SHORT).show();
						}

					}
				});

			}
		}).start();

		dialog.show();
	}
	
	
	public byte[] Bitmap2Bytes(Bitmap bm){
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
		return baos.toByteArray();
	}
}
