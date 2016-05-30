package cn.ucai.superwechat.utils;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import cn.ucai.superwechat.Constant;
import cn.ucai.superwechat.I;
import cn.ucai.superwechat.R;
import cn.ucai.superwechat.SuperWeChatApplication;
import cn.ucai.superwechat.applib.controller.HXSDKHelper;
import cn.ucai.superwechat.DemoHXSDKHelper;
import cn.ucai.superwechat.bean.Contact;
import cn.ucai.superwechat.bean.Group;
import cn.ucai.superwechat.data.RequestManager;
import cn.ucai.superwechat.domain.User;

import com.android.volley.toolbox.NetworkImageView;
import com.easemob.util.HanziToPinyin;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;

public class UserUtils {
    /**
     * 根据username获取相应user，由于demo没有真实的用户数据，这里给的模拟的数据；
     *
     * @param username
     * @return
     */
    public static User getUserInfo(String username) {
        User user = ((DemoHXSDKHelper) HXSDKHelper.getInstance()).getContactList().get(username);
        if (user == null) {
            user = new User(username);
        }

        if (user != null) {
            //demo没有这些数据，临时填充
            if (TextUtils.isEmpty(user.getNick()))
                user.setNick(username);
        }
        return user;
    }

    //取得真实联系人数据  --仿写
    public static Contact getUserBeanInfo(String username) {
        Contact contact = SuperWeChatApplication.getInstance().getUserList().get(username);
        Log.i("main", "UserUtils---------> contact1:  " + contact);
        return contact;
    }

    /**
     * 设置用户头像
     *
     * @param username
     */
    public static void setUserAvatar(Context context, String username, ImageView imageView) {
        User user = getUserInfo(username);
        if (user != null && user.getAvatar() != null) {
            Picasso.with(context).load(user.getAvatar()).placeholder(cn.ucai.superwechat.R.drawable.default_avatar).into(imageView);
        } else {
            Picasso.with(context).load(cn.ucai.superwechat.R.drawable.default_avatar).into(imageView);
        }
    }

    //添加查找好友时的对应用户头像
    public static void setUserBeanAvatar(cn.ucai.superwechat.bean.User user, NetworkImageView imageView) {
        if (user != null && user.getMUserName() != null) {
            //调用自己写的设置用户头像方法
            setUserAvatar(getAvatarPath(user.getMUserName()), imageView);
        } else {
            imageView.setDefaultImageResId(R.drawable.default_avatar);
        }
    }

    //设置真实的用户头像  仿写setUserAvatar(Context context, String username, ImageView imageView)方法
    public static void setUserBeanAvatar(String username, NetworkImageView imageView) {
        Contact contact = getUserBeanInfo(username);
        Log.i("main", "UserUtils---------> contact2:  " + contact);
        if (contact != null && contact.getMContactCname() != null) {
            //调用自己写的设置用户头像方法
            setUserAvatar(getAvatarPath(username), imageView);
        } else {
            imageView.setDefaultImageResId(R.drawable.default_avatar);
        }

    }

    //新加的方法  设置用户头像
    public static void setUserAvatar(String url, NetworkImageView imageView) {
        Log.i("main", "UserUtils---------> url:  " + url);
        if (url == null || url.isEmpty()) return;
        imageView.setDefaultImageResId(R.drawable.default_avatar);
        imageView.setImageUrl(url, RequestManager.getImageLoader());
        imageView.setErrorImageResId(R.drawable.default_avatar);
    }

    //新加的方法  得到用户头像url
    public static String getAvatarPath(String username) {
        Log.i("main", "UserUtils---------> username:  " + username);
        if (username == null || username.isEmpty()) return null;
        return I.REQUEST_DOWNLOAD_AVATAR_USER + username;
    }

    /**
     * 设置当前用户头像
     */
    public static void setCurrentUserAvatar(Context context, ImageView imageView) {
        User user = ((DemoHXSDKHelper) HXSDKHelper.getInstance()).getUserProfileManager().getCurrentUserInfo();
        if (user != null && user.getAvatar() != null) {
            Picasso.with(context).load(user.getAvatar()).placeholder(cn.ucai.superwechat.R.drawable.default_avatar).into(imageView);
        } else {
            Picasso.with(context).load(cn.ucai.superwechat.R.drawable.default_avatar).into(imageView);
        }
    }

    //仿写设置当前用户头像
    public static void setCurrentUserAvatar(NetworkImageView imageView) {
        cn.ucai.superwechat.bean.User user = SuperWeChatApplication.getInstance().getUser();
        if (user != null) {
            //调用仿写的设置用户头像方法
            setUserAvatar(getAvatarPath(user.getMUserName()), imageView);
        }
    }


    /**
     * 设置用户昵称
     */
    public static void setUserNick(String username, TextView textView) {
        User user = getUserInfo(username);

        if (user != null) {
            textView.setText(user.getNick());
        } else {
            textView.setText(username);
        }
    }

    //设置搜索好友昵称
    public static void setUserBeanNick(cn.ucai.superwechat.bean.User user, TextView textView) {
        if (user != null) {
            if (user.getMUserNick() != null) {
                textView.setText(user.getMUserNick());
            } else if (user.getMUserName() != null) {
                textView.setText(user.getMUserName());
            }
        }
    }

    //设置昵称
    public static void setUserBeanNick(String username, TextView textView) {
        Contact contact = getUserBeanInfo(username);
        if (contact != null) {
            if (contact.getMUserNick() != null) {
                textView.setText(contact.getMUserNick());
            } else if (contact.getMContactCname() != null) {
                textView.setText(contact.getMContactCname());
            }
        } else {
            textView.setText(username);
        }

    }

    /**
     * 设置当前用户昵称
     */
    public static void setCurrentUserNick(TextView textView) {
        cn.ucai.superwechat.bean.User user = SuperWeChatApplication.getInstance().getUser();
        if (user != null && textView != null && user.getMUserNick() != null) {
            textView.setText(user.getMUserNick());
        }
    }

    /**
     * 自定义的设置当前用户昵称
     */
    public static void setCurrentUserBeanNick(TextView textView) {
        User user = ((DemoHXSDKHelper) HXSDKHelper.getInstance()).getUserProfileManager().getCurrentUserInfo();
        if (textView != null) {
            textView.setText(user.getNick());
        }
    }

    /**
     * 保存或更新某个用户
     */
    public static void saveUserInfo(User newUser) {
        if (newUser == null || newUser.getUsername() == null) {
            return;
        }
        ((DemoHXSDKHelper) HXSDKHelper.getInstance()).saveContact(newUser);
    }


	//设置群组头像
	public static void setGroupBeanAvatar(String mGroupHxid, NetworkImageView imageView) {
		if (mGroupHxid != null && !mGroupHxid.isEmpty()) {
			setGroupAvatar(getGroupAvatarPath(mGroupHxid), imageView);
		}
	}

	public static String getGroupAvatarPath(String hxid) {
		if (hxid == null || hxid.isEmpty()) return null;
		return I.REQUEST_DOWNLOAD_AVATAR_GROUP + hxid;
	}

	public static void setGroupAvatar(String url, NetworkImageView imageView) {
		if (url == null || url.isEmpty()) return;
		imageView.setDefaultImageResId(R.drawable.group_icon);
		imageView.setImageUrl(url, RequestManager.getImageLoader());
		imageView.setErrorImageResId(R.drawable.group_icon);
	}
	//汉字转拼音
	public static String getPinYinFromHanZi(String hanzi) {
		String pinyin = "";

		for(int i=0;i<hanzi.length();i++){
			String s = hanzi.substring(i,i+1);
			pinyin = pinyin + HanziToPinyin.getInstance()
					.get(s).get(0).target.toLowerCase();
		}
		return pinyin;
	}

	public static Group getGroupBeanFromHxId(String hxid){
		if (hxid!=null && !hxid.isEmpty()){
			ArrayList<Group> groupList = SuperWeChatApplication.getInstance().getGroupList();
			for (Group group: groupList){
				if (group.getMGroupHxid().equals(hxid)) {
					return group;
				}
			}
		}
		return null;
	}


}
