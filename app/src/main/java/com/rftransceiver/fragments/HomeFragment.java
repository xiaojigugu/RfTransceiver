package com.rftransceiver.fragments;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.rftransceiver.R;
import com.rftransceiver.activity.LocationActivity;
import com.rftransceiver.activity.MainActivity;
import com.rftransceiver.adapter.ListConversationAdapter;
import com.rftransceiver.datasets.ConversationData;
import com.rftransceiver.db.DBManager;
import com.rftransceiver.group.GroupEntity;
import com.rftransceiver.util.CommonAdapter;
import com.rftransceiver.util.CommonViewHolder;
import com.rftransceiver.util.Constants;
import com.rftransceiver.util.ExpressionUtil;
import com.rftransceiver.util.ImageUtil;
import com.rftransceiver.util.PoolThreadUtil;
import com.source.DataPacketOptions;


import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.internal.ListenerClass;

/**
 * Created by rantianhua on 15-6-14.
 */
public class HomeFragment extends Fragment implements View.OnClickListener{

    @InjectView(R.id.listview_conversation)
    ListView listView;
    @InjectView(R.id.et_send_message)
    EditText etSendMessage;
    @InjectView(R.id.btn_send)
    Button btnSend;
    @InjectView(R.id.btn_sounds)
    Button btnSounds;
    @InjectView(R.id.img_home_troggle)
    ImageView imgTroggle;
    @InjectView(R.id.img_home_hide)
    ImageView imgHomeHide;
    @InjectView(R.id.tv_tip_home)
    TextView tvTip;
    @InjectView(R.id.img_sounds_text)
    ImageView imgMessageType;
    @InjectView(R.id.img_other)
    ImageView imgAdd;
    @InjectView(R.id.img_home_picture)
    ImageView imgPicture;
    @InjectView(R.id.img_home_address)
    ImageView imgAddress;
    @InjectView(R.id.rl_home_imgs_address)
    RelativeLayout rlOthersData;
    @InjectView(R.id.vp_home_expression)
    ViewPager vp;
    @InjectView(R.id.ll_dots_home)
    LinearLayout llDots;
    @InjectView(R.id.tv_reset_home)
    TextView tvReset;
    @InjectView(R.id.tv_group_info)
    TextView tvSeeGroup;
    @InjectView(R.id.tv_mute_home)
    TextView tvMute;
    @InjectView(R.id.tv_title_content)
    TextView tvTitle;

    /**
     * the reference of callback interface
     */
    private CallbackInHomeFragment callback;

    private ListConversationAdapter conversationAdapter = null; //the adapter of listView

    private List<ConversationData> dataLists;

    /**
     * save all gridView filled with expressions
     */
    private List<GridView> expressions;

    /**
     * current displayed gridview's index in viewpager
     */
    private int currentEpIndex;

    /**
     * save all dots to indicate which gridview is selected in vp
     */
    private List<ImageView> imgDots;

    /**
     * to parse expression
     */
    private Html.ImageGetter imgageGetter;

    /**
     * the object animation for right menu
     */
    private AnimatorSet translateIn,translateOut;
    private float transOffset;

    /**
     * a instance of a GroupEntity
     */
    private GroupEntity groupEntity;

    private Editable.Factory editableFactory = Editable.Factory.getInstance();

    private Rect rect = new Rect();

    private int imgSendSize;

    private String tipConnectLose,tipReconnecting,tipConnecSuccess;

    /**
     * to manipulate database
     */
    private DBManager dbManager;

    private String homeTitle;

    private enum MenuAction{
        MUTE,
        GROUP_INFO,
        RESET,
        NONE
    }
    private MenuAction menuAction = MenuAction.NONE;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        expressions = new ArrayList<>();
        imgDots = new ArrayList<>();
        initImageGetter();
        transOffset = getResources().getDisplayMetrics().density * 80 + 0.5f;
//        imgSendSize = getResources().getDimensionPixelSize(R.dimen.img_data_height)
//             * getResources().getDimensionPixelSize(R.dimen.img_data_width);

        imgSendSize = 40 * 80;
        tipConnectLose = getResources().getString(R.string.connection_lose);
        tipReconnecting = getResources().getString(R.string.reconnecting);
        tipConnecSuccess = getResources().getString(R.string.connect_success);
        dataLists = new ArrayList<>();
        dbManager = DBManager.getInstance(getActivity());
     }

    private void initExpressions(LayoutInflater inflater) {
        for(int i = 0; i < ExpressionUtil.epDatas.size();i ++) {
            GridView gridView = (GridView)inflater.inflate(R.layout.grid_expressiona,null);
            gridView.setAdapter(new CommonAdapter<Integer>(getActivity(),
                    ExpressionUtil.epDatas.get(i),R.layout.grid_expressions_item) {
                @Override
                public void convert(CommonViewHolder helper, Integer item) {
                    helper.setImageResource(R.id.img_expression,item);
                }
            });
            gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    //get expression's id
                    int epId = ExpressionUtil.epDatas.get(currentEpIndex).get(i);
                    insertExpression(epId,etSendMessage);
                    if(btnSounds.getVisibility() == View.VISIBLE) {
                        btnSounds.setVisibility(View.INVISIBLE);
                        etSendMessage.setVisibility(View.VISIBLE);
                    }
                }
            });
            expressions.add(gridView);
            ImageView imgDot = (ImageView)inflater.inflate(R.layout.img_dot,null);
            if(i == 0) {
                imgDot.setSelected(true);
            }
            llDots.addView(imgDot);
            imgDots.add(imgDot);
        }
        vp.setAdapter(new PagerAdapter() {
            @Override
            public int getCount() {
                return expressions.size();
            }

            @Override
            public boolean isViewFromObject(View view, Object object) {
                return view == object;
            }

            @Override
            public Object instantiateItem(ViewGroup container, int position) {
                container.addView(expressions.get(position));
                return expressions.get(position);
            }

            @Override
            public void destroyItem(ViewGroup container, int position, Object object) {
                container.removeView(expressions.get(position));
            }
        });
        vp.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                imgDots.get(currentEpIndex).setSelected(false);
                currentEpIndex = position;
                imgDots.get(position).setSelected(true);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }

        });
        vp.setCurrentItem(0);
    }


    /**
     * insert expression to EditText
     * @param drawableId
     */
    private void insertExpression(int drawableId,View view) {
        String source = "<img src='" + drawableId+ "'/>";
        CharSequence cs = Html.fromHtml(source,imgageGetter,null);
        if(view instanceof EditText) {
            EditText editText = (EditText)view;
            editText.append(cs);
        }else if(view instanceof TextView) {
            TextView tv = (TextView)view;
            tv.append(cs);
        }
    }

    private void initImageGetter() {
        imgageGetter = new Html.ImageGetter() {
            @Override
            public Drawable getDrawable(String s) {
                int id = Integer.parseInt(s);
                Drawable drawable = getResources().getDrawable(id);
                drawable.setBounds(0,0,40,
                        40);
                return drawable;
            }
        };
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_home_content,container,false);
        initView(v);
        initExpressions(inflater);
        initEvent();
        return v;
    }

    private void initView(View v) {
        ButterKnife.inject(this,v);
        conversationAdapter = new ListConversationAdapter(getActivity(),imgageGetter,getFragmentManager());
        listView.setAdapter(conversationAdapter);
        if(!TextUtils.isEmpty(homeTitle)) {
            tvTitle.setText(homeTitle);
            homeTitle = null;
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if(groupEntity == null) {
            int preGrop = getActivity().getSharedPreferences(Constants.SP_USER,0).getInt(Constants.PRE_GROUP,-1);
            if(preGrop != -1) {
                loadGroup(preGrop);
            }
        }
    }

    private void initEvent() {
        btnSend.setOnClickListener(this);
        btnSounds.setOnClickListener(this);
        imgTroggle.setOnClickListener(this);
        imgHomeHide.setOnClickListener(this);
        tvTip.setOnClickListener(this);
        imgMessageType.setOnClickListener(this);
        imgAdd.setOnClickListener(this);
        imgPicture.setOnClickListener(this);
        imgAddress.setOnClickListener(this);
        tvReset.setOnClickListener(this);
        tvMute.setOnClickListener(this);
        tvSeeGroup.setOnClickListener(this);

        etSendMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if(editable.toString().length() > 0) {
                    imgAdd.setVisibility(View.INVISIBLE);
                    btnSend.setVisibility(View.VISIBLE);
                }else{
                    imgAdd.setVisibility(View.VISIBLE);
                    btnSend.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_send:
                sendText();
                break;
            case R.id.btn_sounds:
                if(btnSounds.getText().equals(getString(R.string.record_sound))) {
                    sendSounds();
                }
                else if(btnSounds.getText().equals(getString(R.string.recording_sound))) {
                    if(callback != null) callback.stopSendSounds();
                    btnSounds.setText(getString(R.string.record_sound));
                }
                break;
            case R.id.img_home_troggle:
                if(callback != null) callback.toggleMenu();
                break;
            case R.id.img_home_hide:
                //click hide menu on the top
                imgHomeHide.setClickable(false);
                if(tvMute.getVisibility() == View.INVISIBLE) {
                    showRightMenu();
                }else {
                    hideRightMenu();
                }
                break;
            case R.id.tv_tip_home:
                if(tvTip.getText().toString().equals(tipConnectLose)) {
                    if(callback != null) {
                        callback.reconnectDevice();
                        tvTip.setText(tipReconnecting);
                    }
                }
                break;
            case R.id.img_sounds_text:
                //click to change send message type
                if(imgMessageType.isSelected()) {
                    imgMessageType.setSelected(false);
                    etSendMessage.setVisibility(View.VISIBLE);
                    btnSounds.setVisibility(View.INVISIBLE);
                }else {
                    imgMessageType.setSelected(true);
                    etSendMessage.setVisibility(View.INVISIBLE);
                    btnSounds.setVisibility(View.VISIBLE);
                    btnSend.setVisibility(View.INVISIBLE);
                    imgAdd.setVisibility(View.VISIBLE);
                }
                break;
            case R.id.img_other:
                //want to send other data,address or picture
                if(imgAdd.isSelected()) {
                    imgAdd.setSelected(false);
                    rlOthersData.setVisibility(View.GONE);
                }else {
                    imgAdd.setSelected(true);
                    rlOthersData.setVisibility(View.VISIBLE);
                }
                break;
            case R.id.img_home_picture:
                //want to send picture
                ImagesFragment imagesFragment = ImagesFragment.getInstance(REQUEST_HOME);
                imagesFragment.setTargetFragment(HomeFragment.this,REQUEST_HOME);
                getFragmentManager().beginTransaction().replace(R.id.frame_content,
                        imagesFragment)
                        .addToBackStack(null)
                        .commit();
                break;
            case R.id.img_home_address:
                //want to send address
                Intent intent = new Intent();
                intent.setClass(getActivity(),LocationActivity.class);
                getActivity().startActivityForResult(intent, REQUEST_LOCATION);
                break;
            case R.id.tv_mute_home:
                hideRightMenu();
                menuAction = MenuAction.MUTE;
                break;
            case R.id.tv_group_info:
                menuAction = MenuAction.GROUP_INFO;
                hideRightMenu();
                break;
            case R.id.tv_reset_home:
                menuAction = MenuAction.RESET;
                hideRightMenu();
                break;
            default:
                break;
        }
    }

    /**
     * show right menu with translate animation
     */
    private void showRightMenu() {
        initInAnimation();
        translateIn.start();
    }

    private void initInAnimation() {
        if(translateIn != null) return;
        translateIn = new AnimatorSet();
        List<Animator> animators = new ArrayList<>();
        ObjectAnimator transTvResetIn = ObjectAnimator.ofFloat(tvReset,"translationX",transOffset,0.0f);
        transTvResetIn.setDuration(300);
        transTvResetIn.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                tvReset.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animator) {

            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        ObjectAnimator transTvGroupIn = ObjectAnimator.ofFloat(tvSeeGroup,"translationX",transOffset,0.0f);
        transTvGroupIn.setDuration(300);
        transTvGroupIn.setStartDelay(150);
        transTvGroupIn.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                tvSeeGroup.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animator) {

            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        ObjectAnimator transTvMuteIn = ObjectAnimator.ofFloat(tvMute,"translationX",transOffset,0.0f);
        transTvMuteIn.setDuration(300);
        transTvMuteIn.setStartDelay(300);
        transTvMuteIn.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                tvMute.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                imgHomeHide.setClickable(true);
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        animators.add(transTvResetIn);
        animators.add(transTvGroupIn);
        animators.add(transTvMuteIn);
        translateIn.playTogether(animators);
    }

    @Override
    public void onPause() {
        super.onPause();
        translateIn = null;
        translateOut = null;
    }

    /**
     * hide right menu with animation
     */
    private void hideRightMenu() {
        initOutAnimation();
        translateOut.start();
    }

    private void initOutAnimation() {
        if(translateOut != null) return;
        translateOut = new AnimatorSet();
        List<Animator> animators = new ArrayList<>();
        ObjectAnimator transTvResetOut = ObjectAnimator.ofFloat(tvReset,"translationX",transOffset);
        transTvResetOut.setDuration(300);
        transTvResetOut.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                tvReset.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        ObjectAnimator transTvGroupOut = ObjectAnimator.ofFloat(tvSeeGroup,"translationX",transOffset);
        transTvGroupOut.setStartDelay(150);
        transTvGroupOut.setDuration(300);
        transTvGroupOut.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                tvSeeGroup.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        ObjectAnimator transTvMuteOut = ObjectAnimator.ofFloat(tvMute,"translationX",0.0f,transOffset);
        transTvMuteOut.setDuration(300);
        transTvMuteOut.setStartDelay(300);
        transTvMuteOut.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                tvMute.setVisibility(View.INVISIBLE);
                imgHomeHide.setClickable(true);
                switch (menuAction) {
                    case MUTE:
                        break;
                    case GROUP_INFO:
                        if(groupEntity != null && groupEntity.getMembers().size() > 0) {
                            getFragmentManager().beginTransaction().replace(R.id.frame_content,
                                    GroupDetailFragment.getInstance(groupEntity))
                                    .addToBackStack(null)
                                    .commit();
                        }
                        break;
                    case RESET:
                        if(callback != null) {
                            callback.resetCms();
                        }
                        break;
                }
                menuAction = MenuAction.NONE;
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        animators.add(transTvResetOut);
        animators.add(transTvGroupOut);
        animators.add(transTvMuteOut);
        translateOut.playTogether(animators);
    }

    public void setCallback(CallbackInHomeFragment callback) {
        this.callback = callback;
    }

    /**
     * send text
     */
    private void sendText() {
        Editable editable = editableFactory.newEditable(etSendMessage.getText());
        String message = Html.toHtml(editable);
        Log.e("sendText",message);
        message = message.replace("<p>","");
        message = message.replace("</p>", "");
        message = message.replace("</br>", "");
        message = message.replace("<br>", "");
        Log.e("sendText",message);
        if(!TextUtils.isEmpty(message)) {
            if(callback != null) {
                callback.send(MainActivity.SendAction.Words,message);
            }
        }
    }

    /**
     * send sounds
     */
    private void sendSounds() {
        if(callback != null){
            callback.send(MainActivity.SendAction.SOUNDS,null);
        }
    }

    /**
     * is receiving sounds or text data
     * @param tye 0 is sounds data
     *            1 is words data
     *            2 is address data
     *            3 is image data
     */
    public void receivingData(int tye,String data) {
        ConversationData receiveData = null;
        switch (tye) {
            case 0:
                btnSounds.setText(getString(R.string.sounds_im));
                btnSounds.setClickable(false);
                receiveData = new ConversationData(ListConversationAdapter.ConversationType.LEFT_SOUNDS,
                        null,BitmapFactory.decodeResource(getResources(),R.drawable.photo),0,"100m");
                break;
            case 1:
                receiveData = new ConversationData(ListConversationAdapter.ConversationType.LEFT_TEXT,
                        data, BitmapFactory.decodeResource(getResources(),R.drawable.photo),0,"100m");
                break;
            case 2:
                receiveData = new ConversationData(ListConversationAdapter.ConversationType.LEFT_ADDRESS,
                        null, BitmapFactory.decodeResource(getResources(),R.drawable.photo),0,"100m");
                receiveData.setAddress(data);
                break;
            case 3:
                receiveData = new ConversationData(ListConversationAdapter.ConversationType.LEFT_PIC,
                        null, BitmapFactory.decodeResource(getResources(),R.drawable.photo),0,"100m");
                receiveData.setBitmap(data);
                break;
        }
        if(receiveData == null) return;
        dataLists.add(receiveData);
        conversationAdapter.updateData(dataLists);
        listView.setSelection(conversationAdapter.getCount()-1);
    }

    /**
     * after reveive all data
     * @param type 0 is sounds data,
     */
    public void endReceive(int type) {
        if(type == 0) {
            btnSounds.setText(getString(R.string.record_sound));
            btnSounds.setClickable(true);
        }
    }

    /**
     * call if can send text by ble
     * @param sendText the text wait to be send
     */
    public void sendText(String sendText,DataPacketOptions.TextType type) {
        if(TextUtils.isEmpty(sendText)) return;
        ConversationData subData = null;
        switch (type) {
            case Words:
                hideSoft();
                etSendMessage.setText("");
                subData = new ConversationData(ListConversationAdapter.ConversationType.RIGHT_TEXT,
                        sendText);
                break;
            case Address:
                subData = new ConversationData(ListConversationAdapter.ConversationType.RIGHT_ADDRESS,
                        null);
                subData.setAddress(sendText);
                break;
            case Image:
                subData = new ConversationData(ListConversationAdapter.ConversationType.RIGHT_PIC,
                        null);
                subData.setBitmap(sendText);
                break;
        }
        if(subData == null) return;
        dataLists.add(subData);
        conversationAdapter.updateData(dataLists);
        listView.setSelection(conversationAdapter.getCount()-1);
    }

    private void hideSoft() {
        InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(etSendMessage.getWindowToken(), 0);
    }

    public void reset() {
        btnSounds.setText(getString(R.string.record_sound));
        btnSounds.setClickable(true);
        btnSend.setClickable(true);
    }

    /**
     * call when starting recording sounds
     */
    public void startSendingSounds() {
        btnSounds.setText(getString(R.string.recording_sound));
    }

    /**
     * called to check the viewpager is touched or not
     * @param touchX
     * @param touchY
     * @return
     */
    public boolean checkTouch(int touchX, int touchY) {
        if(tvMute.getVisibility() == View.VISIBLE) {
            tvReset.getGlobalVisibleRect(rect);
            if(rect.contains(touchX,touchY)) return false;
            tvSeeGroup.getGlobalVisibleRect(rect);
            if(rect.contains(touchX,touchY)) return false;
            tvMute.getGlobalVisibleRect(rect);
            if(rect.contains(touchX,touchY)) return false;
            imgHomeHide.getGlobalVisibleRect(rect);
            if(rect.contains(touchX,touchY)) return false;
            hideRightMenu();
            //do not dispath touch event
            return true;
        }
        vp.getGlobalVisibleRect(rect);
        if(rect.contains(touchX,touchY)) {
            //tell parent do not intercept touch event
            vp.getParent().requestDisallowInterceptTouchEvent(true);
        }
        return false;
    }

    public void deviceConnected(final boolean connect) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(connect) {
                    if(tvTip.getText().toString().equals(tipReconnecting) && tvTip.getVisibility() ==
                            View.VISIBLE) {
                        tvTip.setText(tipConnecSuccess);
                        tvTip.setVisibility(View.GONE);
                    }
                }else {
                    if(!tvTip.getText().toString().equals(tipReconnecting)) {
                        tvTip.setText(tipConnectLose);
                        tvTip.setVisibility(View.VISIBLE);
                    }
                }
            }
        });
    }

    /**
     * update the group of talk
     * @param groupEntity
     */
    public void updateGroup(final GroupEntity groupEntity) {
        this.groupEntity = groupEntity;
        if(groupEntity == null) return;
        final String name = groupEntity.getName();
        if(tvTitle == null) {
            homeTitle = name
                    +"(" + groupEntity.getMembers().size() + "人" + ")";
        }else {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tvTitle.setText(name
                            +"(" + groupEntity.getMembers().size() + "人" + ")");
                }
            });
        }
    }

    public interface CallbackInHomeFragment {
        /**
         * send text or sound message
         */
        void send(MainActivity.SendAction sendAction,String text);

        /**
         * stop send sounds
         */
        void stopSendSounds();

        /**
         * call to open or close menu
         */
        void toggleMenu();

        /**
         * call to reconnect device
         */
        void reconnectDevice();

        /**
         * call to reset cms
         */
        void resetCms();
    }

    @Override
    public void onDestroy() {
        dbManager = null;
        super.onDestroy();
        expressions = null;
        imgDots = null;
        dataLists = null;
    }


    /**
     * select a group from database by gid
     */
    private void loadGroup(final int gid) {
        PoolThreadUtil.getInstance().addTask(new Runnable() {
            @Override
            public void run() {
                Log.e("loadGroup","to load exists group");
                updateGroup(dbManager.getAgroup(gid));
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_HOME && resultCode == Activity.RESULT_CANCELED && data != null) {
            getFragmentManager().popBackStackImmediate();
            String imgPath = data.getStringExtra(Constants.PHOTO_PATH);
            if(imgPath == null) return;
            Bitmap bitmap = ImageUtil.createImageThumbnail(imgPath,imgSendSize);
            if(bitmap == null) return;
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int options = 100;
            bitmap.compress(Bitmap.CompressFormat.PNG,options,outputStream);
            while (outputStream.toByteArray().length / 1024 > 60) {
                outputStream.reset();
                options -= 10;
                bitmap.compress(Bitmap.CompressFormat.PNG,options,outputStream);
            }
            String imgData = Base64.encodeToString(outputStream.toByteArray(),Base64.DEFAULT);
            if(callback != null) {
                callback.send(MainActivity.SendAction.Image,imgData);
            }
        }else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public static final int REQUEST_LOCATION = 302;
    public static final int REQUEST_HOME = 303;
    public static final String EXTRA_LOCATION = "address";
}
