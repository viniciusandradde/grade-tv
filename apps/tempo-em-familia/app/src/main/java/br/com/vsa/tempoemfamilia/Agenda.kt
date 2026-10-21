package br.com.vsa.tempoemfamilia

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import org.json.JSONArray
import java.util.Calendar

/**
 * Guarda os horários de bloqueio e agenda um alarme exato para o próximo
 * início ou fim de bloqueio. No alarme, o app abre sozinho (AlarmReceiver).
 *
 * Formato (JSON, o mesmo da interface): [{"days":[1,2,3],"start":"21:00","end":"07:00"}, ...]
 */
object Agenda {
    private const val PREFS = "tempoemfamilia"
    private const val KEY = "horarios"

    fun salvar(ctx: Context, json: String) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, json).apply()
        reagendar(ctx)
    }

    fun ler(ctx: Context): String =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]") ?: "[]"

    private fun hm(s: String): Int { val p = s.split(":"); return p[0].toInt() * 60 + p[1].toInt() }

    /** Próximo instante (ms) em que um bloqueio começa ou termina, depois de agora. */
    fun proximoEvento(ctx: Context, agora: Long = System.currentTimeMillis()): Long? {
        val arr = try { JSONArray(ler(ctx)) } catch (_: Exception) { return null }
        var melhor: Long? = null
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val dias = o.getJSONArray("days"); val ini = hm(o.getString("start")); val fim = hm(o.getString("end"))
            val durMin = if (fim > ini) fim - ini else 24 * 60 - ini + fim
            for (d in -1..7) {
                val c = Calendar.getInstance().apply {
                    timeInMillis = agora; add(Calendar.DAY_OF_YEAR, d)
                    set(Calendar.HOUR_OF_DAY, ini / 60); set(Calendar.MINUTE, ini % 60); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
                var bate = false
                for (k in 0 until dias.length()) if (dias.getInt(k) == c.get(Calendar.DAY_OF_WEEK) - 1) bate = true
                if (!bate) continue
                val inicio = c.timeInMillis; val termino = inicio + durMin * 60_000L
                for (t in listOf(inicio, termino)) if (t > agora && (melhor == null || t < melhor!!)) melhor = t
            }
        }
        return melhor
    }

    fun reagendar(ctx: Context) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(
            ctx, 1, Intent(ctx, AlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
        )
        am.cancel(pi)
        val quando = proximoEvento(ctx) ?: return
        val exato = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
        try {
            if (exato) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, quando, pi)
            else am.setWindow(AlarmManager.RTC_WAKEUP, quando, 60_000L, pi)
        } catch (_: SecurityException) {
            am.setWindow(AlarmManager.RTC_WAKEUP, quando, 60_000L, pi)
        }
    }
}
