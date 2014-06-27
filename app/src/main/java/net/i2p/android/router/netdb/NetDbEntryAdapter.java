package net.i2p.android.router.netdb;

import java.util.List;

import net.i2p.android.router.R;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class NetDbEntryAdapter extends ArrayAdapter<NetDbEntry> {
    private final LayoutInflater mInflater;

    public NetDbEntryAdapter(Context context) {
        super(context, android.R.layout.simple_list_item_2);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void setData(List<NetDbEntry> entries) {
        clear();
        if (entries != null) {
            for (NetDbEntry entry : entries) {
                add(entry);
            }
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v;
        NetDbEntry entry = getItem(position);

        if (entry.isRouterInfo()) {
            v = mInflater.inflate(R.layout.listitem_routerinfo, parent, false);

            int countryIcon = entry.getCountryIcon();
            if (countryIcon > 0) {
                ImageView country = (ImageView) v.findViewById(R.id.ri_country);
                country.setImageDrawable(getContext().getResources()
                        .getDrawable(countryIcon));
            }
        } else {
            v = mInflater.inflate(R.layout.listitem_leaseset, parent, false);

            TextView nickname = (TextView) v.findViewById(R.id.ls_nickname);
            nickname.setText(entry.getNickname());
        }

        TextView hash = (TextView) v.findViewById(R.id.dbentry_hash);
        hash.setText(entry.getHash().toBase64());

        return v;
    }
}
