package io.github.wzw0101.android.stockmarket;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class NewsDialog extends DialogFragment {

    private NewsInfo info;

    public static NewsDialog getInstance(NewsInfo info) {
        NewsDialog dialog = new NewsDialog();
        dialog.info = info;
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_news, null);
        builder.setView(view);

        ((TextView) view.findViewById(R.id.textViewArticleSource)).setText(info.source);
        LocalDate date = Instant.ofEpochSecond(info.datetime).atZone(ZoneId.systemDefault()).toLocalDate();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("LLLL d, y");
        ((TextView) view.findViewById(R.id.textViewArticleDate)).setText(date.format(formatter));
        ((TextView) view.findViewById(R.id.textViewArticleTitle)).setText(info.headline);
        ((TextView) view.findViewById(R.id.textViewArticleSummary)).setText(info.summary);

        view.findViewById(R.id.btnChrome).setOnClickListener(v -> startActivity(new Intent().setAction(Intent.ACTION_VIEW).setData(Uri.parse(info.url))));
        view.findViewById(R.id.btnTwitter).setOnClickListener(v -> startActivity(new Intent().setAction(Intent.ACTION_VIEW).setData(Uri.parse("https://twitter.com/intent/tweet?url=" + info.url))));
        view.findViewById(R.id.btnFb).setOnClickListener(v -> startActivity(new Intent().setAction(Intent.ACTION_VIEW).setData(Uri.parse("https://www.facebook.com/sharer/sharer.php?u=" + info.url))));
        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        return dialog;
    }
}
