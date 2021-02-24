package dev.dworks.apps.awatch.helper;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.wear.widget.WearableRecyclerView;
import dev.dworks.apps.awatch.R;
import dev.dworks.apps.awatch.common.config.ConfigHelper;
import dev.dworks.apps.awatch.common.config.Themes;

public class ColorAdapter extends WearableRecyclerView.Adapter {
    private static final int TYPE_NORMAL = 1;
    private static final int TYPE_MUZEI = 2;
    private final SharedPreferences mSharedPreferences;
    private Activity mActivity;
    private boolean mHasArtWork;

    public ColorAdapter(Activity activity, boolean hasArtWork){
        mActivity = activity;
        mHasArtWork = hasArtWork;
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mActivity);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.config_theme_color_item, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public int getItemViewType(int position) {
        return (position >= Themes.THEMES.length) ? TYPE_MUZEI : TYPE_NORMAL;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ItemViewHolder itemHolder = (ItemViewHolder) holder;
        itemHolder.onBind(getItemViewType(position), position);
    }

    @Override
    public int getItemCount() {
        return Themes.THEMES.length + (mHasArtWork ? 1 : 0);
    }

    public class ItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private ImageView circleView;

        public ItemViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            circleView = (ImageView) itemView.findViewById(R.id.circle);
        }

        public void onBind(int type, int position){
            Themes.Theme theme;
            if (type== TYPE_MUZEI) {
                theme = Themes.MUZEI_THEME;
                circleView.setImageResource(R.drawable.muzei_icon);
            } else {
                theme = Themes.THEMES[position];
                ((GradientDrawable) circleView.getDrawable()).setColor(
                        ContextCompat.getColor(mActivity,theme.defaultRes));
            }
            itemView.setTag(theme.id);
        }

        @Override
        public void onClick(View v) {
            String theme = itemView.getTag().toString();
            mSharedPreferences.edit().putString(ConfigHelper.KEY_THEME, theme).apply();
            // Let's Complication Config Activity know there was an update to colors.
            mActivity.setResult(Activity.RESULT_OK);
            mActivity.finish();
        }
    }
}
