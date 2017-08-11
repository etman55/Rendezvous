package com.example.android.rendezvous.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.android.rendezvous.R;
import com.example.android.rendezvous.models.User;
import com.example.android.rendezvous.utils.CircularTransform;
import com.example.android.rendezvous.utils.FontCache;
import com.example.android.rendezvous.utils.PicassoCache;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by Etman on 8/5/2017.
 */

public class PeopleAdapter extends RecyclerView.Adapter<PeopleAdapter.PeopleViewHolder> {
    private List<User> userList = new ArrayList<>();
    private ItemClickHandler itemClickHandler;

    public PeopleAdapter(List<User> userList) {
        this.userList = userList;
    }

    public void setItemClickHandler(ItemClickHandler itemClickHandler) {
        this.itemClickHandler = itemClickHandler;
    }

    @Override
    public PeopleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);
        return new PeopleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final PeopleViewHolder holder, final int position) {
        if (userList.size() != 0) {
            final User user = userList.get(position);
            holder.setName(user.getName());
            holder.setmAbout(user.getAbout());
            holder.setUserAvatar(user.getThumb_image(), holder.itemView.getContext());
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    itemClickHandler.onItem(position, user.getThumb_image());
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public class PeopleViewHolder extends RecyclerView.ViewHolder {
        @Bind(R.id.user_avatar)
        ImageView userAvatar;
        @Bind(R.id.user_name_txt)
        TextView mDisplayName;
        @Bind(R.id.user_about_txt)
        TextView mAbout;
        @Bind(R.id.user_status_img)
        ImageView onlineStatus;
        final Typeface tf;
        final Typeface tf1;

        public PeopleViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            onlineStatus.setVisibility(View.INVISIBLE);
            tf = FontCache.get("fonts/Aller_Rg.ttf", itemView.getContext());
            tf1 = FontCache.get("fonts/Aller_Lt.ttf", itemView.getContext());
            mDisplayName.setTypeface(tf);
            mAbout.setTypeface(tf1);

        }

        public void setName(String name) {
            mDisplayName.setText(name);
        }

        public void setmAbout(String about) {
            mAbout.setText(about);
        }

        public void setUserAvatar(final String thumbUrl, final Context context) {
            if (thumbUrl != null && !TextUtils.isEmpty(thumbUrl)) {
                PicassoCache.get(context)
                        .load(thumbUrl)
                        .networkPolicy(NetworkPolicy.OFFLINE)
                        .placeholder(R.drawable.ic_place_holder)
                        .resizeDimen(R.dimen.user_avatar_size, R.dimen.user_avatar_size)
                        .centerCrop()
                        .transform(new CircularTransform())
                        .into(userAvatar, new Callback() {
                            @Override
                            public void onSuccess() {

                            }

                            @Override
                            public void onError() {
                                Picasso.with(context)
                                        .load(thumbUrl)
                                        .placeholder(R.drawable.ic_place_holder)
                                        .resizeDimen(R.dimen.user_avatar_size, R.dimen.user_avatar_size)
                                        .centerCrop()
                                        .transform(new CircularTransform())
                                        .into(userAvatar);
                            }
                        });
            }
        }
    }

    public interface ItemClickHandler {
        void onItem(int position, String thumb);
    }
}
