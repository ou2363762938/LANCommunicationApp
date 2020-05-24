package com.skysoft.smart.intranetchat.ui.activity.userinfoshow;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.skysoft.smart.intranetchat.bean.signal.AvatarSignal;
import com.skysoft.smart.intranetchat.bean.signal.ContactSignal;
import com.skysoft.smart.intranetchat.model.avatar.AvatarManager;
import com.skysoft.smart.intranetchat.model.contact.ContactManager;
import com.skysoft.smart.intranetchat.tools.GsonTools;
import com.skysoft.smart.intranetchat.tools.toastutil.TLog;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.skysoft.smart.intranetchat.R;
import com.skysoft.smart.intranetchat.app.IntranetChatApplication;
import com.skysoft.smart.intranetchat.model.login.Login;
import com.skysoft.smart.intranetchat.database.table.ContactEntity;
import com.skysoft.smart.intranetchat.model.network.Config;
import com.skysoft.smart.intranetchat.tools.customstatusbar.CustomStatusBarBackground;
import com.skysoft.smart.intranetchat.ui.activity.chatroom.ChatRoom.ChatRoomConfig;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import de.hdodenhof.circleimageview.CircleImageView;

public class UserInfoShowActivity extends AppCompatActivity {
    private static String TAG = UserInfoShowActivity.class.getSimpleName();
    private ViewHolder mHolder;
    private ContactEntity mContact;
    private int mContactId;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_user_info_show);
        CustomStatusBarBackground.customStatusBarTransparent(this);
        CustomStatusBarBackground.drawableViewStatusBar(this,R.drawable.custom_gradient_main_title,findViewById(R.id.custom_status_bar_background));
        EventBus.getDefault().register(this);

        mHolder.avatar = findViewById(R.id.user_info_head);
        mHolder.name = findViewById(R.id.user_info_name);
        mHolder.state = findViewById(R.id.user_info_state);

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        mContactId = bundle.getInt(ChatRoomConfig.NAME);
        mContact = ContactManager.getInstance().getContact(mContactId);
        ContactManager.getInstance().setInUserInfoShowActivity(true);

        if (null != mContact){
            setStatus();
        }

        if (!TextUtils.isEmpty(mContact.getHost())){
            Login.requestUserInfo(mContact.getHost());
        }

        mHolder.name.setText(mContact.getName());

        AvatarManager
                .getInstance()
                .loadContactAvatar(this,mHolder.avatar,mContact.getAvatar());
    }

    public class ViewHolder{
        CircleImageView avatar;
        TextView name;
        TextView state;
    }

    public static void go(Context context,int contact){
        Intent intent = new Intent(context, UserInfoShowActivity.class);
        Bundle bundle = new Bundle();
        bundle.putInt(ChatRoomConfig.NAME,contact);
        intent.putExtras(bundle);
        context.startActivity(intent);
    }

    private void setStatus() {
        switch (mContact.getStatus()){
            case Config.STATUS_ONLINE:
                mHolder.state.setText(getString(R.string.user_state_online));
                mHolder.state.setTextColor(getResources().getColor(R.color.color_green));
                break;
            case Config.STATUS_BUSY:
                mHolder.state.setText(getString(R.string.user_state_busy));
                mHolder.state.setTextColor(getResources().getColor(R.color.color_red));
                break;
            case Config.STATUS_OUT_LINE:
                mHolder.state.setText(getString(R.string.user_state_out_line));
                mHolder.state.setTextColor(getResources().getColor(R.color.color_gray));
                break;
            default:
                TLog.d(TAG, "onCreate: next.status() = " + mContact.getStatus());
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void receiveContactSignal(ContactSignal signal) {
        AvatarManager.
                getInstance().
                loadContactAvatar(
                UserInfoShowActivity.this,
                        mHolder.avatar,
                        mContact.getAvatar());
        mHolder.name.setText(mContact.getName());
        setStatus();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void receiveAvatarSignal(AvatarSignal signal) {
        if (mContact.getIdentifier().equals(signal)) {
            AvatarManager.
                    getInstance().
                    loadContactAvatar(
                            UserInfoShowActivity.this,
                            mHolder.avatar,
                            mContact.getAvatar());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ContactManager.getInstance().setInUserInfoShowActivity(false);
        EventBus.getDefault().unregister(this);
    }
}
