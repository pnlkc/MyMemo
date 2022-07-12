package com.pnlkc.mymemo.util

import android.app.AlertDialog
import android.content.Context

class DialogCreator {
    fun showDialog(
        context: Context,
        title: String,
        message: String,
        action: () -> Unit
    ) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("확인") { _, _ ->
                action()
            }
            .setNegativeButton("취소") { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .setOnCancelListener { dialogInterface ->
                dialogInterface.dismiss()
            }
            .create()
            .show()
    }

    fun showDialog(
        context: Context,
        title: String,
        message: String,
        positiveAction: () -> Unit,
        negativeAction: () -> Unit
    ) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("확인") { _, _ ->
                positiveAction()
            }
            .setNegativeButton("취소") { _, _ ->
                negativeAction()
            }
            .setOnCancelListener {
                negativeAction()
            }
            .create()
            .show()
    }
}