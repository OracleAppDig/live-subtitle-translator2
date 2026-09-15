package com.example.livepersiansubtitle

import android.app.*
import android.content.*
import android.graphics.*
import android.media.*
import android.media.projection.*
import android.os.*
import android.view.*
import android.widget.*
import com.google.mlkit.genai.common.*
import com.google.mlkit.genai.common.audio.AudioSource
import com.google.mlkit.genai.speechrecognition.*
import com.google.mlkit.translate.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import java.io.*
import java.util.Locale

class CaptureService: Service() {
 private var projection: MediaProjection?=null
 private var recorder: AudioRecord?=null
 private var writer: ParcelFileDescriptor.AutoCloseOutputStream?=null
 private var readerPfd: ParcelFileDescriptor?=null
 private var writerPfd: ParcelFileDescriptor?=null
 private var recognizer: SpeechRecognizer?=null
 private var translator: Translator?=null
 private var wm: WindowManager?=null
 private var tv: TextView?=null
 private var audioThread: Thread?=null
 private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Default)
 @Volatile private var running=false

 override fun onCreate(){
  super.onCreate()
  val nm=getSystemService(NOTIFICATION_SERVICE) as NotificationManager
  nm.createNotificationChannel(NotificationChannel("live","زیرنویس زنده",NotificationManager.IMPORTANCE_LOW))
  startForeground(1,Notification.Builder(this,"live").setContentTitle("زیرنویس زنده فارسی").setContentText("فعال").setSmallIcon(android.R.drawable.ic_media_play).build())
  show("در حال آماده‌سازی مدل...")
  val opt=TranslatorOptions.Builder().setSourceLanguage(TranslateLanguage.ENGLISH).setTargetLanguage(TranslateLanguage.PERSIAN).build()
  translator=Translation.getClient(opt)
  translator!!.downloadModelIfNeeded(DownloadConditions.Builder().requireWifi().build())
   .addOnSuccessListener{show("مدل آماده است؛ ویدئو را پخش کن")}
   .addOnFailureListener{show("دانلود مدل ترجمه ناموفق بود")}
  overlay()
 }

 override fun onStartCommand(i:Intent?,flags:Int,startId:Int):Int{
  val d=i?.getParcelableExtra<Intent>("data") ?: return START_NOT_STICKY
  val code=i.getIntExtra("code",-1)
  projection=(getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager).getMediaProjection(code,d)
  if(projection==null){show("MediaProjection فعال نشد");return START_NOT_STICKY}
  startAudioPipe()
  return START_NOT_STICKY
 }

 private fun startAudioPipe(){
  if(running)return
  val cfg=AudioPlaybackCaptureConfiguration.Builder(projection!!)
   .addMatchingUsage(AudioAttributes.USAGE_MEDIA).build()
  val fmt=AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
   .setSampleRate(16000).setChannelMask(AudioFormat.CHANNEL_IN_MONO).build()
  val min=AudioRecord.getMinBufferSize(16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT)
  recorder=AudioRecord.Builder().setAudioFormat(fmt).setBufferSizeInBytes(maxOf(min*2,32768))
   .setAudioPlaybackCaptureConfig(cfg).build()
  val pipe=ParcelFileDescriptor.createPipe()
  readerPfd=pipe[0];writerPfd=pipe[1]
  writer=ParcelFileDescriptor.AutoCloseOutputStream(writerPfd)
  running=true
  recorder!!.startRecording()
  startRecognizer(readerPfd!!)
  audioThread=Thread{
   val buf=ByteArray(3200)
   try{
    while(running){
     val n=recorder!!.read(buf,0,buf.size)
     if(n>0) writer!!.write(buf,0,n)
    }
   }catch(_:Exception){}
  }.also{it.start()}
 }

 private fun startRecognizer(pfd:ParcelFileDescriptor){
  val options=speechRecognizerOptions {
   locale=Locale.US
   preferredMode=SpeechRecognizerOptions.Mode.MODE_BASIC
  }
  recognizer=SpeechRecognition.getClient(options)
  scope.launch {
   try{
    val status=recognizer!!.checkStatus()
    if(status==FeatureStatus.DOWNLOADABLE){
     recognizer!!.download().collect{}
    }
    if(status==FeatureStatus.UNAVAILABLE){
     withContext(Dispatchers.Main){show("تشخیص گفتار روی این گوشی در دسترس نیست")}
     return@launch
    }
    val request=speechRecognizerRequest { audioSource=AudioSource.fromPfd(pfd) }
    recognizer!!.startRecognition(request).collect{resp->
     when(resp){
      is SpeechRecognizerResponse.PartialTextResponse -> translate(resp.text)
      is SpeechRecognizerResponse.FinalTextResponse -> translate(resp.text)
      is SpeechRecognizerResponse.ErrorResponse -> withContext(Dispatchers.Main){show("خطای تشخیص گفتار")}
      else -> {}
     }
    }
   }catch(e:Exception){
    withContext(Dispatchers.Main){show("مدل گفتار آماده نشد")}
   }
  }
 }

 private fun translate(text:String){
  if(text.isBlank())return
  translator?.translate(text)?.addOnSuccessListener{show(it)}
 }

 private fun overlay(){
  wm=getSystemService(WINDOW_SERVICE) as WindowManager
  tv=TextView(this).apply{
   text="زیرنویس زنده فارسی";textSize=19f;setTextColor(Color.WHITE)
   gravity=Gravity.CENTER;setPadding(24,12,24,12);setBackgroundColor(Color.argb(205,0,0,0))
  }
  val p=WindowManager.LayoutParams(-1,-2,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
   WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,PixelFormat.TRANSLUCENT)
  p.gravity=Gravity.BOTTOM
  wm!!.addView(tv,p)
 }

 private fun show(s:String){Handler(Looper.getMainLooper()).post{tv?.text=s}}

 override fun onDestroy(){
  running=false
  audioThread?.interrupt()
  try{recorder?.stop()}catch(_:Exception){}
  recorder?.release();recorder=null
  try{writer?.close()}catch(_:Exception){}
  try{readerPfd?.close()}catch(_:Exception){}
  try{recognizer?.stopRecognition()}catch(_:Exception){}
  recognizer?.close();recognizer=null
  translator?.close();translator=null
  projection?.stop();projection=null
  scope.cancel()
  try{tv?.let{wm?.removeView(it)}}catch(_:Exception){}
  super.onDestroy()
 }
 override fun onBind(i:Intent?):IBinder?=null
}