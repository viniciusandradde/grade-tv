package br.com.vsa.tempoemfamilia

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Disparado no início/fim de cada bloqueio: abre o app e agenda o próximo evento. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Agenda.reagendar(context)
        val abrir = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            .putExtra("alarme", true)
        try { context.startActivity(abrir) } catch (_: Exception) {}
    }
}
