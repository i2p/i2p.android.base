package net.i2p.android.router;

import android.content.res.Resources;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.i2p.android.apps.NewsFetcher;
import net.i2p.android.router.util.Util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

public class NewsFragment extends I2PFragmentBase {
    private long _lastChanged;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_news, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        NewsFetcher nf = NewsFetcher.getInstance();
        if (nf != null) {
            // Always update the status
            TextView tv = (TextView) getActivity().findViewById(R.id.news_status);
            tv.setText(nf.status().replace("&nbsp;", " "));
            tv.setVisibility(View.VISIBLE);
        }

        // Only update the content if we need to
        File newsFile = new File(Util.getFileDir(getActivity()), "docs/news.xml");
        boolean newsExists = newsFile.exists();
        if (_lastChanged > 0 && ((!newsExists) || newsFile.lastModified() < _lastChanged))
            return;
        _lastChanged = System.currentTimeMillis();

        InputStream in = null;
        ByteArrayOutputStream out = new ByteArrayOutputStream(2048);
        byte buf[] = new byte[1024];
        try {
            if (newsExists) {
                in = new FileInputStream(newsFile);
            } else {
                in = getResources().openRawResource(R.raw.initialnews_html);
            }

            int read;
            while ( (read = in.read(buf)) != -1)
                out.write(buf, 0, read);

        } catch (IOException ioe) {
            System.err.println("news error " + ioe);
        } catch (Resources.NotFoundException nfe) {
        } finally {
            if (in != null) try { in.close(); } catch (IOException ioe) {}
        }

        String news = "";
        try {
            news = out.toString("UTF-8");
        } catch (UnsupportedEncodingException uee) {
        }

        // Get SpannableStringBuilder object from HTML code
        CharSequence sequence = Html.fromHtml(news);
        SpannableStringBuilder strBuilder = new SpannableStringBuilder(sequence);

        // Get an array of URLSpan from SpannableStringBuilder object
        URLSpan[] urlSpans = strBuilder.getSpans(0, strBuilder.length(), URLSpan.class);

        // Remove URLSpans with relative paths, which can't be clicked on
        for (final URLSpan span : urlSpans) {
            if (span.getURL().startsWith("/"))
                strBuilder.removeSpan(span);
        }

        TextView tv = (TextView) getActivity().findViewById(R.id.news_content);
        tv.setText(strBuilder);
        tv.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
