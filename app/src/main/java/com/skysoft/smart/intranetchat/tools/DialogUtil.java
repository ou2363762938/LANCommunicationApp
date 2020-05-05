package com.skysoft.smart.intranetchat.tools;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.Map;

public class DialogUtil {
    public static AlertDialog createDialog(Context context,
                                           String title, String message,
                                           String positive, final DialogInterface.OnClickListener positiveListener,
                                           String negative, final DialogInterface.OnClickListener negativeListener) {
        AlertDialog dialog =  new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positive,positiveListener)
                .setNegativeButton(negative,negativeListener)
                .create();
        dialog.setCanceledOnTouchOutside(true);
        return dialog;
    }

    public static AlertDialog createDialog(Context context, View view,
                                           String title,
                                           String positive, final DialogInterface.OnClickListener positiveListener,
                                           String negative, final DialogInterface.OnClickListener negativeListener) {
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(title)
                .setView(view)
                .setPositiveButton(positive,positiveListener)
                .setNegativeButton(negative,negativeListener)
                .create();
        dialog.setCanceledOnTouchOutside(true);
        return dialog;
    }

    public static AlertDialog createListDialog(Context context,
                                               String title, String[] items,
                                               final DialogInterface.OnClickListener listener) {
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(title)
                .setItems(items, listener)
                .create();
        dialog.setCanceledOnTouchOutside(true);
        return dialog;
    }

    public static AlertDialog createListDialog(Context context,
                                               String title, final int itemsId,
                                               final DialogInterface.OnClickListener clickListener) {
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(title)
                .setItems(itemsId, clickListener)
                .create();
        dialog.setCanceledOnTouchOutside(true);
        return dialog;
    }

}
