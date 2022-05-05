package io.github.wzw0101.android.stockmarket;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class TradeResultDialog extends DialogFragment {

    private static final String TEMPLATE = "You have successfully %s shares of %s";

    private int shares;
    private boolean buy;
    private String symbol;

    public static TradeResultDialog getInstance(int shares, boolean buy, String symbol) {
        TradeResultDialog dialog = new TradeResultDialog();
        dialog.shares = shares;
        dialog.buy = buy;
        dialog.symbol = symbol;
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_trade_result, null);
        builder.setView(view);

        TextView textViewMessage = view.findViewById(R.id.textViewMessage);
        if (buy) {
            textViewMessage.setText(String.format(TEMPLATE, "bought " + shares, symbol));
        } else {
            textViewMessage.setText(String.format(TEMPLATE, "sold " + shares, symbol));
        }

        view.findViewById(R.id.btnDone).setOnClickListener(v -> this.dismiss());

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        return dialog;
    }
}
