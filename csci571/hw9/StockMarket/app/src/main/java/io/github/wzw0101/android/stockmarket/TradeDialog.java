package io.github.wzw0101.android.stockmarket;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.fragment.app.DialogFragment;

public class TradeDialog extends DialogFragment {
    private static final String TAG = TradeDialog.class.toString();

    private String symbol, name;
    private double price, d, dp;

    private Preferences preferences;

    private int shares;
    private TextView textViewComputation;
    private TextView editTextShares;

    public static TradeDialog getInstance(String symbol, String name, double price, double d, double dp) {
        TradeDialog dialog = new TradeDialog();
        dialog.price = price;
        dialog.symbol = symbol;
        dialog.name = name;
        dialog.d = d;
        dialog.dp = dp;
        return dialog;
    }

    public interface NoticeDialogListener {
        void onBtnClicked();
    }

    private NoticeDialogListener listener;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        preferences = Preferences.getInstance(requireContext());

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_trade, null);
        ((TextView) view.findViewById(R.id.textViewTradeDialogTitle)).setText(String.format("Trade %s Shares", name));
        ((TextView) view.findViewById(R.id.textViewBalance)).setText(String.format("$%.2f to buy %s", preferences.getBalance(), symbol));
        textViewComputation = view.findViewById(R.id.textViewComputation);
        setTextViewComputation();

        editTextShares = view.findViewById(R.id.editTextShares);
        editTextShares.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    shares = Integer.parseInt(s.toString());
                } catch (NumberFormatException e) {
                    shares = 0;
                }
                setTextViewComputation();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        view.findViewById(R.id.btnBuy).setOnClickListener(v -> {
            String value = editTextShares.getText().toString();
            if (!isValid(value)) {
                Toast.makeText(getActivity(), "Please enter a valid amount", Toast.LENGTH_SHORT).show();
                return;
            }
            if (shares <= 0) {
                Toast.makeText(getActivity(), "Cannot buy non-positive shares", Toast.LENGTH_SHORT).show();
                return;
            }
            if (preferences.canBuy(price, shares)) {
                preferences.buyStock(requireContext(), symbol, price, shares, d, dp);
                dismiss();
                listener.onBtnClicked();
                TradeResultDialog.getInstance(shares, true, symbol).show(getParentFragmentManager(), "result");
            } else {
                Toast.makeText(getActivity(), "Not enough money to buy", Toast.LENGTH_SHORT).show();
            }
        });
        view.findViewById(R.id.btnSell).setOnClickListener(v -> {
            String value = editTextShares.getText().toString();
            if (!isValid(value)) {
                Toast.makeText(getActivity(), "Please enter a valid amount", Toast.LENGTH_SHORT).show();
                return;
            }
            if (shares <= 0) {
                Toast.makeText(getActivity(), "Cannot sell non-positive shares", Toast.LENGTH_SHORT).show();
                return;
            }
            if (preferences.canSell(symbol, shares)) {
                preferences.sellStock(requireContext(), symbol, price, shares);
                dismiss();
                listener.onBtnClicked();
                TradeResultDialog.getInstance(shares, false, symbol).show(getParentFragmentManager(), "result");
            } else {
                Toast.makeText(getActivity(), "Not enough shares to sell", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setView(view);

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        return dialog;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        listener = (NoticeDialogListener) context;
    }

    private void setTextViewComputation() {
        textViewComputation.setText(String.format("%.1f*$%.2f/share = %.2f", (float) shares, price, shares * price));
    }

    private boolean isValid(String value) {
        if (null == value || "".equals(value)) {
            return false;
        }
        try {
            Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }
}
