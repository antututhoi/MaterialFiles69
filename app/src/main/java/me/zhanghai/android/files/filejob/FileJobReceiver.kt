/*
 * Copyright (c) 2019 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.filejob

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import me.zhanghai.android.files.app.application

class FileJobReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (val action = intent.action) {
            ACTION_CANCEL -> {
                val jobId = intent.getIntExtra(EXTRA_JOB_ID, 0)
                FileJobService.cancelJob(jobId)
            }
            // 69 mod
            ACTION_PAUSE -> {
                val jobId = intent.getIntExtra(EXTRA_JOB_ID, 0)
                me.zhanghai.android.files.mod69.Mod69Pause.toggle(jobId)
                android.widget.Toast.makeText(
                    context,
                    if (me.zhanghai.android.files.mod69.Mod69Pause.isPaused(jobId)) "Paused" else "Resumed",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
            else -> throw IllegalArgumentException(action)
        }
    }

    companion object {
        private const val ACTION_CANCEL = "cancel"
        private const val ACTION_PAUSE = "mod69_pause"

        fun createPauseIntent(jobId: Int): Intent =
            Intent(application, FileJobReceiver::class.java)
                .setAction(ACTION_PAUSE)
                .putExtra(EXTRA_JOB_ID, jobId)

        private const val EXTRA_JOB_ID = "jobId"

        fun createIntent(jobId: Int): Intent =
            Intent(application, FileJobReceiver::class.java)
                .setAction(ACTION_CANCEL)
                .putExtra(EXTRA_JOB_ID, jobId)
    }
}
