package com.example.android.rendezvous.adapters;

import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.android.rendezvous.R;
import com.example.android.rendezvous.models.Message;
import com.example.android.rendezvous.utils.FontCache;
import com.example.android.rendezvous.utils.PicassoCache;
import com.google.firebase.auth.FirebaseAuth;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private FirebaseAuth mAuth;
    private ArrayList<Message> mMessageList = new ArrayList<>();

    public MessageAdapter(ArrayList<Message> mMessageList) {
        this.mMessageList = mMessageList;
        mAuth = FirebaseAuth.getInstance();

    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(viewType == 0 ? R.layout.item_message : R.layout.item_message_sender, parent, false);
        if (viewType == 0) {
            return new ReceiverViewHolder(v);
        } else {
            return new SenderViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Message msg = mMessageList.get(position);
        if (getItemViewType(position) == 1) {
            if (msg.getType().equals("image"))
                ((SenderViewHolder) holder).setMsgImg(msg.getMessage());
            else
                ((SenderViewHolder) holder).setMsgTv(msg.getMessage());
        } else if (getItemViewType(position) == 0) {
            if (msg.getType().equals("image"))
                ((ReceiverViewHolder) holder).setOtherMsgImg(msg.getMessage());
            else
                ((ReceiverViewHolder) holder).setOtherMsgTv(msg.getMessage());
        }
    }

    @Override
    public int getItemViewType(int position) {
        String userId = mAuth.getCurrentUser().getUid();
        Message msg = mMessageList.get(position);
        String fromUser = msg.getFrom();
        if (fromUser.equals(userId)) {
            return 1; // sender
        } else {
            return 0; // receiver
        }
    }

    @Override
    public int getItemCount() {
        return mMessageList.size();
    }

    public class ReceiverViewHolder extends RecyclerView.ViewHolder {
        @Bind(R.id.other_msg_tv)
        TextView msgTv;
        @Bind(R.id.other_image_msg)
        ImageView msgImg;
        @Bind(R.id.progress_other)
        ProgressBar progressBar;
        Typeface tf1;

        public ReceiverViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            tf1 = FontCache.get("fonts/Aller_Lt.ttf", itemView.getContext());
            msgTv.setTypeface(tf1);
        }


        private void setOtherMsgTv(String msg) {
            msgImg.setVisibility(View.INVISIBLE);
            msgTv.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.INVISIBLE);
            msgTv.setText(msg);
        }

        private void setOtherMsgImg(final String msg) {
            msgTv.setVisibility(View.INVISIBLE);
            msgImg.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.VISIBLE);
            PicassoCache.get(itemView.getContext())
                    .load(msg)
                    .networkPolicy(NetworkPolicy.OFFLINE)
                    .into(msgImg, new Callback() {
                        @Override
                        public void onSuccess() {
                            progressBar.setVisibility(View.INVISIBLE);
                        }

                        @Override
                        public void onError() {
                            Picasso.with(itemView.getContext())
                                    .load(msg)
                                    .into(msgImg);
                        }
                    });
        }
    }

    public class SenderViewHolder extends RecyclerView.ViewHolder {
        @Bind(R.id.my_image_msg)
        ImageView msgImage;
        @Bind(R.id.my_msg_tv)
        TextView msgTv;
        @Bind(R.id.progress_sender)
        ProgressBar progressBar;
        Typeface tf1;

        public SenderViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            tf1 = FontCache.get("fonts/Aller_Lt.ttf", itemView.getContext());
            msgTv.setTypeface(tf1);

        }

        private void setMsgTv(String msg) {
            msgImage.setVisibility(View.INVISIBLE);
            msgTv.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.INVISIBLE);
            msgTv.setText(msg);
        }

        private void setMsgImg(final String msg) {
            msgTv.setVisibility(View.INVISIBLE);
            msgImage.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.VISIBLE);
            PicassoCache.get(itemView.getContext())
                    .load(msg)
                    .networkPolicy(NetworkPolicy.OFFLINE)
                    .into(msgImage, new Callback() {
                        @Override
                        public void onSuccess() {
                            progressBar.setVisibility(View.INVISIBLE);
                        }

                        @Override
                        public void onError() {
                            Picasso.with(itemView.getContext())
                                    .load(msg)
                                    .into(msgImage);
                        }
                    });
        }
    }

}
