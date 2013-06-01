package com.mcxiaoke.fanfouapp.util;

import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.ForegroundColorSpan;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.util.Log;
import android.widget.TextView;
import android.widget.TextView.BufferType;
import com.mcxiaoke.fanfouapp.R;
import com.mcxiaoke.fanfouapp.app.AppContext;
import com.mcxiaoke.fanfouapp.dao.model.StatusModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @author mcxiaoke
 * @version 2.0 2012.03.18
 */
public class StatusHelper {

    public static class URLSpanNoUnderline extends URLSpan {
        public URLSpanNoUnderline(String url) {
            super(url);
        }

        @Override
        public void updateDrawState(TextPaint tp) {
            super.updateDrawState(tp);
            tp.setUnderlineText(false);
        }
    }

    private static final String TAG = "StatusHelper";

    private static HashMap<String, String> userNameIdMap = new HashMap<String, String>();

    private static final Pattern PATTERN_USER = Pattern.compile("@.+?\\s");
    private static final Linkify.MatchFilter MATCH_FILTER_USER = new Linkify.MatchFilter() {
        @Override
        public final boolean acceptMatch(final CharSequence s, final int start,
                                         final int end) {
            String name = s.subSequence(start + 1, end).toString().trim();
            return userNameIdMap.containsKey(name);
        }
    };

    private static final Linkify.TransformFilter TRANSFORM_USER = new Linkify.TransformFilter() {

        @Override
        public String transformUrl(Matcher match, String url) {
            String name = url.subSequence(1, url.length()).toString().trim();
            return userNameIdMap.get(name);
        }
    };

    private static final String SCHEME_USER = "fanfouapp://profile/";

    public static void linkifyUsers(TextView view) {
        Linkify.addLinks(view, PATTERN_USER, SCHEME_USER, MATCH_FILTER_USER,
                TRANSFORM_USER);
    }

    private static final Pattern PATTERN_SEARCH = Pattern.compile("#\\w+#");

    private static final Linkify.TransformFilter TRANSFORM_SEARCH = new Linkify.TransformFilter() {
        @Override
        public final String transformUrl(Matcher match, String url) {
            String result = url.substring(1, url.length() - 1);
            return result;
        }
    };

    private static final String SCHEME_SEARCH = "fanfouapp://search/";

    public static void linkifyTags(TextView view) {
        Linkify.addLinks(view, PATTERN_SEARCH, SCHEME_SEARCH, null,
                TRANSFORM_SEARCH);
    }

    private static Pattern PATTERN_USERLINK = Pattern
            .compile("<a href=\"http:\\/\\/fanfou\\.com\\/(.*?)\" class=\"former\">(.*?)<\\/a>");

    private static String preprocessText(String text) {
        // 处理HTML格式返回的用户链接
        Matcher m = PATTERN_USERLINK.matcher(text);
        while (m.find()) {
            userNameIdMap.put(m.group(2), m.group(1));
            if (AppContext.DEBUG) {
                Log.d(TAG, "preprocessText() screenName=" + m.group(2)
                        + " userId=" + m.group(1));
            }
        }
        // 将User Link的连接去掉
        // StringBuffer sb = new StringBuffer();
        // m = PATTERN_USERLINK.matcher(text);
        // while (m.find()) {
        // m.appendReplacement(sb, "@$2");
        // }
        // m.appendTail(sb);
        // if(App.DEBUG){
        // Log.d(TAG, "preprocessText() result="+sb.toString());
        // }
        // return sb.toString();

        return Html.fromHtml(text).toString();
    }

    public static void setStatus(final TextView textView, final String text) {
        String processedText = preprocessText(text);
//        Spannable spannable=Spannable.Factory.getInstance().newSpannable(processedText);
        textView.setText(processedText, BufferType.SPANNABLE);
        Linkify.addLinks(textView, Linkify.WEB_URLS);
        linkifyUsers(textView);
        linkifyTags(textView);
        removeUnderlines((Spannable) textView.getText());
        userNameIdMap.clear();
    }


    public static void setItemStatus(final TextView textView, final String text) {
        SpannableStringBuilder spannable = new SpannableStringBuilder(text);
        Matcher m = PATTERN_USER.matcher(spannable);
        while (m.find()) {
            int start = m.start();
            int end = m.end();
            spannable.setSpan(new ForegroundColorSpan(textView.getResources().getColor(R.color.holo_primary)), start,
                    end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//            spannable.setSpan(new StyleSpan(Typeface.BOLD), start, end,
//                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        Linkify.addLinks(spannable, Linkify.WEB_URLS);
        removeUnderlines(spannable);
        textView.setText(spannable);
    }

    public static void removeUnderlines(Spannable s) {
        URLSpan[] spans = s.getSpans(0, s.length(), URLSpan.class);
        for (URLSpan span : spans) {
            int start = s.getSpanStart(span);
            int end = s.getSpanEnd(span);
            s.removeSpan(span);
            span = new URLSpanNoUnderline(span.getURL());
            s.setSpan(span, start, end, 0);
        }
    }

    /**
     * 从消息中获取全部提到的人，将它们按先后顺序放入一个列表
     *
     * @param text
     * 消息文本
     * @return 消息中@的人的列表，按顺序存放
     */
    private static final Pattern namePattern = Pattern.compile("@(.*?)\\s");
    private static final int MAX_NAME_LENGTH = 12;

    public static ArrayList<String> getMentions(final StatusModel status) {
        String text = status.getSimpleText();
        ArrayList<String> names = new ArrayList<String>();
        names.add(status.getUserScreenName());
        Matcher m = namePattern.matcher(text);
        while (m.find()) {
            String name = m.group(1);
            if (!names.contains(name) && name.length() <= MAX_NAME_LENGTH + 1) {
                names.add(m.group(1));
            }
        }
        String name = AppContext.getScreenName();
        names.remove(name);
        return names;
    }

}
