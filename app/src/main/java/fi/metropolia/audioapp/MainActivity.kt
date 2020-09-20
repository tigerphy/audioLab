package fi.metropolia.audioapp

import android.Manifest
import android.content.pm.PackageManager
import android.media.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.MotionEvent
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import java.io.*
import android.media.AudioTrack
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity(), Runnable, View.OnTouchListener, View.OnClickListener {

    private var isRecording = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        recButton.setOnTouchListener(this)
        listenButton.setOnClickListener(this)
        setupPermissions()
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isRecording = true
                recStautsTv.text = "recording..."
                Thread(this).start()
            }
            MotionEvent.ACTION_UP -> {
                recStautsTv.text = ""
                isRecording = false
            }
        }
        return v.performClick()
    }

    override fun onClick(v: View?) {
        val runnable = Runnable {
            val inputStream = resources.openRawResource(R.raw.song)
            val minBufferSize = AudioTrack.getMinBufferSize(
                44100,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT)

            val aAttr: AudioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            val aFormat: AudioFormat = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(44100)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                .build()
            val track= AudioTrack.Builder()
                .setAudioAttributes(aAttr)
                .setAudioFormat(aFormat)
                .setBufferSizeInBytes(minBufferSize)
                .build()
            track.setVolume(0.2f)
            track.play()

            var i: Int
            val buffer = ByteArray(minBufferSize)
            try {
                i = inputStream.read(buffer, 0, minBufferSize)
                while (i != -1) {
                    track.write(buffer, 0, i)
                    i = inputStream.read(buffer, 0, minBufferSize)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }

            try {
                inputStream.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            track.stop()
            track.release()
        }
        val myThread = Thread(runnable)
        myThread.start()
    }

    override fun run() {
        val recFileName = "my-recording.raw"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        lateinit var recFile: File
        try {
            recFile = File("$storageDir/$recFileName")
        } catch (e: IOException) {
            Log.e("MainActivity", "Failed to create the file.\n$e")
        }

        try {
            val outputStream = FileOutputStream(recFile)
            val bufferedOutputStream = BufferedOutputStream(outputStream)
            val dataOutputStream = DataOutputStream(bufferedOutputStream)
            val minBufferSize = AudioRecord.getMinBufferSize(
                44100,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT)
            val audioFormat = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(44100)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                .build()
            val recorder = AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.MIC)
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(minBufferSize)
                .build()
            val audioData = ByteArray(minBufferSize)
            recorder.startRecording()

            while (isRecording) {
                val numOfBytes = recorder.read(audioData, 0, minBufferSize)
                if (numOfBytes > 0) {
                    dataOutputStream.write(audioData)
                }
            }
            recorder.stop()
            dataOutputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun setupPermissions() {
        val permission = ContextCompat.checkSelfPermission(this,
            Manifest.permission.RECORD_AUDIO)

        if (permission != PackageManager.PERMISSION_GRANTED) {
            Log.i("MainActivity", "Permission to record denied")
            makeRequest()
        }
    }

    private fun makeRequest() {
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.RECORD_AUDIO), 101)
    }
}
