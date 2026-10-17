package br.com.vsa.gradetv

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Abre a Grade TV quando a TV termina de ligar.
 * Útil em aparelhos (Google TV, Fire TV) que não deixam trocar a tela inicial:
 * a criança liga a TV e já cai na Grade TV.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val abrir = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(abrir)
        } catch (_: Exception) {
            // Em algumas versões do Android apps não podem abrir atividades direto do boot;
            // nesse caso o usuário escolhe a Grade TV como tela inicial.
        }
    }
}
