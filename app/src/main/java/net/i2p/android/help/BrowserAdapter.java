package net.i2p.android.help;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.net.Uri;
//import android.support.v7.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import net.i2p.android.router.R;

public class BrowserAdapter extends RecyclerView.Adapter<BrowserAdapter.ViewHolder> {
    private Context mCtx;
    private Browser[] mBrowsers;
    private OnBrowserSelectedListener mListener;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView mIcon;
        public TextView mLabel;
        public ImageView mStatus;

        public ViewHolder(View v) {
            super(v);
            mIcon = (ImageView) v.findViewById(R.id.browser_icon);
            mLabel = (TextView) v.findViewById(R.id.browser_label);
            mStatus = (ImageView) v.findViewById(R.id.browser_status_icon);
        }
    }

    public interface OnBrowserSelectedListener {
        void onBrowserSelected(Browser browser);
    }

    public BrowserAdapter(Context ctx, OnBrowserSelectedListener listener) {
        mCtx = ctx;
        mListener = listener;
    }

    public void setBrowsers(Browser[] browsers) {
        mBrowsers = browsers;
        notifyDataSetChanged();
    }

    public void clear() {
        mBrowsers = null;
        notifyDataSetChanged();
    }

    // Create new views (invoked by the layout manager)
    @Override
    public BrowserAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                        int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.listitem_browser, parent, false);
        return new ViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final Browser browser = mBrowsers[position];
        holder.mIcon.setImageDrawable(browser.icon);
        holder.mLabel.setText(browser.label);

        if (browser.isKnown) {
            if (browser.isRecommended && browser.isInstalled(mCtx)) {
                holder.mStatus.setImageDrawable(
                        mCtx.getResources().getDrawable(R.drawable.ic_stars_white_24dp));
                holder.mStatus.setVisibility(View.VISIBLE);
            } else if (browser.isSupported && browser.isInstalled(mCtx)) {
                holder.mStatus.setImageDrawable(
                        mCtx.getResources().getDrawable(R.drawable.ic_stars_white_24dp));
                holder.mStatus.setVisibility(View.INVISIBLE);
            } else if (browser.isSupported && !browser.isInstalled(mCtx)) {
                holder.mStatus.setImageDrawable(
                        mCtx.getResources().getDrawable(R.drawable.ic_shop_white_24dp));
                holder.mStatus.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String uriMarket = "market://search?q=pname:" + browser.packageName;
                        Uri uri = Uri.parse(uriMarket);
                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                        try {
                            mCtx.startActivity(intent);
                        } catch (ActivityNotFoundException e) {
                            Toast.makeText(mCtx, R.string.no_market_app, Toast.LENGTH_LONG).show();
                        }
                    }
                });
                holder.mStatus.setVisibility(View.VISIBLE);
            } else if (browser.isInstalled(mCtx) && !browser.isSupported) {
                // Make the icon gray-scale to show it is unsupported
                ColorMatrix matrix = new ColorMatrix();
                matrix.setSaturation(0);
                ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
                holder.mIcon.setColorFilter(filter);
                holder.mLabel.setTextColor(
                        mCtx.getResources().getColor(R.color.primary_text_disabled_material_dark));
            }
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.onBrowserSelected(browser);
            }
        });
    }

    // Return the size of the dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        if (mBrowsers != null)
            return mBrowsers.length;
        return 0;
    }
}
