package com.example.sweepneko

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf

object SoundManager {
    private var mainPlayer: MediaPlayer? = null
    private var bossPlayer: MediaPlayer? = null
    private var menuPlayer: MediaPlayer? = null
    private var gameOverPlayer: MediaPlayer? = null
    
    private var appContext: Context? = null

    private var soundPool: SoundPool? = null
    private val sounds = mutableMapOf<String, Int>()
    private var ultStreamId: Int = 0

    private val _bgmVolume = mutableStateOf(0.7f)
    val bgmVolumeSnapshot: State<Float> = _bgmVolume
    
    private val _sfxVolume = mutableStateOf(1.0f)
    val sfxVolumeSnapshot: State<Float> = _sfxVolume

    fun init(context: Context) {
        val appContextLocal = context.applicationContext
        appContext = appContextLocal
        if (soundPool != null) return

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        
        soundPool = SoundPool.Builder()
            .setMaxStreams(10)
            .setAudioAttributes(audioAttributes)
            .build()

        sounds["slash"] = soundPool!!.load(appContextLocal, R.raw.mc_slash, 1)
        sounds["use"] = soundPool!!.load(appContextLocal, R.raw.mc_use, 1)
        sounds["bomb"] = soundPool!!.load(appContextLocal, R.raw.mc_bomb, 1)
        sounds["takedamage"] = soundPool!!.load(appContextLocal, R.raw.takedamage, 1)
        sounds["mama"] = soundPool!!.load(appContextLocal, R.raw.mama, 1)
        sounds["ult"] = soundPool!!.load(appContextLocal, R.raw.ult, 1)

        menuPlayer = MediaPlayer.create(appContextLocal, R.raw.mc_menu).apply { isLooping = true }
        mainPlayer = MediaPlayer.create(appContextLocal, R.raw.mc_main).apply { isLooping = true }
        bossPlayer = MediaPlayer.create(appContextLocal, R.raw.mc_boss).apply { isLooping = true }
        gameOverPlayer = MediaPlayer.create(appContextLocal, R.raw.gameover).apply { isLooping = false }
        
        updateVolumes()
    }

    fun playSFX(name: String) {
        val soundId = sounds[name] ?: return
        val volume = when (name) {
            "slash" -> _sfxVolume.value * 1.5f
            "takedamage" -> _sfxVolume.value * 2.0f
            "ult" -> _sfxVolume.value * 2.5f
            else -> _sfxVolume.value
        }
        soundPool?.play(soundId, minOf(1.0f, volume), minOf(1.0f, volume), 1, 0, 1f)
    }

    fun playMenuMusic() {
        stopAllMusic()
        menuPlayer?.start()
    }

    fun playMainMusic() {
        stopAllMusic()
        mainPlayer?.let {
            it.setVolume(_bgmVolume.value, _bgmVolume.value)
            it.start()
        }
    }

    fun playBossMusic() {
        if (bossPlayer?.isPlaying == true) return
        
        mainPlayer?.setVolume(_bgmVolume.value * 0.2f, _bgmVolume.value * 0.2f)
        bossPlayer?.let {
            it.setVolume(_bgmVolume.value, _bgmVolume.value)
            it.start()
        }
    }

    fun stopBossMusic() {
        if (bossPlayer?.isPlaying == true) {
            bossPlayer?.pause()
            bossPlayer?.seekTo(0)
            mainPlayer?.setVolume(_bgmVolume.value, _bgmVolume.value)
        }
    }

    fun playGameOverMusic() {
        stopAllMusic()
        gameOverPlayer?.let {
            it.seekTo(0)
            val vol = minOf(1.0f, _bgmVolume.value * 2.5f)
            it.setVolume(vol, vol)
            it.start()
        }
    }

    fun playUltMusic() {
        val soundId = sounds["ult"] ?: return
        val vol = minOf(1.0f, _sfxVolume.value * 2.5f)
        ultStreamId = soundPool?.play(soundId, vol, vol, 1, 0, 1f) ?: 0
    }

    fun stopUltMusic() {
        if (ultStreamId != 0) {
            soundPool?.stop(ultStreamId)
            ultStreamId = 0
        }
    }

    fun stopAllMusic() {
        if (ultStreamId != 0) {
            soundPool?.stop(ultStreamId)
            ultStreamId = 0
        }
        listOf(menuPlayer, mainPlayer, bossPlayer, gameOverPlayer).forEach {
            if (it?.isPlaying == true) {
                it.pause()
                it.seekTo(0)
            }
        }
    }
    
    fun setBGMVolume(volume: Float) {
        _bgmVolume.value = volume
        updateVolumes()
    }
    
    fun setSFXVolume(volume: Float) {
        _sfxVolume.value = volume
    }
    
    private fun updateVolumes() {
        val vol = _bgmVolume.value
        menuPlayer?.setVolume(vol, vol)
        mainPlayer?.setVolume(vol, vol)
        bossPlayer?.setVolume(vol, vol)
        val goVol = minOf(1.0f, vol * 2.5f)
        gameOverPlayer?.setVolume(goVol, goVol)
    }

    fun getContext(): Context? = appContext

    fun release() {
        stopAllMusic()
        menuPlayer?.release()
        mainPlayer?.release()
        bossPlayer?.release()
        gameOverPlayer?.release()
        soundPool?.release()
        
        menuPlayer = null
        mainPlayer = null
        bossPlayer = null
        gameOverPlayer = null
        soundPool = null
    }
}
