package io.twentysixty.rn.eidreader

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Build
import android.provider.Settings
import android.util.Base64
import android.util.Log
import com.facebook.react.bridge.ActivityEventListener
import com.facebook.react.bridge.LifecycleEventListener
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.modules.core.DeviceEventManagerModule
import io.twentysixty.rn.eidreader.dto.MrzInfo
import io.twentysixty.rn.eidreader.utils.BitmapUtil
import io.twentysixty.rn.eidreader.utils.JsonToReactMap
import io.twentysixty.rn.eidreader.utils.serializeToMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jmrtd.BACKey
import org.jmrtd.BACKeySpec
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import jj2000.j2k.decoder.Decoder
import jj2000.j2k.util.ParameterList
import org.jmrtd.lds.AbstractImageInfo
import java.io.IOException


class EIdReaderModule(reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext), LifecycleEventListener, ActivityEventListener {

  private val nfcPassportReader = EIdReader(reactContext)
  private var adapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(reactContext)
  private var mrzInfo: MrzInfo? = null
  private var includeImages = false
  private var includeRawData = false
  private var isReading = false
  private val jsonToReactMap = JsonToReactMap()
  private var _promise: Promise? = null
  private var _dialog: AlertDialog? = null

  init {
    reactApplicationContext.addLifecycleEventListener(this)
    reactApplicationContext.addActivityEventListener(this)

    val filter = IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)
    reactApplicationContext.registerReceiver(NfcStatusReceiver(), filter)
  }

  inner class NfcStatusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
      if (NfcAdapter.ACTION_ADAPTER_STATE_CHANGED == intent?.action) {
        val state = intent.getIntExtra(NfcAdapter.EXTRA_ADAPTER_STATE, NfcAdapter.STATE_OFF)
        when (state) {
          NfcAdapter.STATE_OFF -> {
            sendEvent("onNfcStateChanged", "off")
          }

          NfcAdapter.STATE_ON -> {
            sendEvent("onNfcStateChanged", "on")
          }

          NfcAdapter.STATE_TURNING_OFF -> {
            // NFC kapanıyor
          }

          NfcAdapter.STATE_TURNING_ON -> {
            // NFC açılıyor
          }
        }
      }
    }
  }

  override fun getName(): String {
    return NAME
  }

  override fun onHostResume() {
    try {
      adapter?.let {
        currentActivity?.let { activity ->
          val intent = Intent(activity, activity.javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
          }

          val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent
              .getActivity(
                activity, 0,
                intent,
                PendingIntent.FLAG_MUTABLE
              )
          } else {
            PendingIntent
              .getActivity(
                activity, 0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
              )
          }

          val filter = arrayOf(arrayOf("android.nfc.tech.IsoDep"))

          it.enableForegroundDispatch(
            activity,
            pendingIntent,
            null,
            filter
          )
        } ?: run {
          Log.e("EIdReader", "CurrentActivity is null")
        }
      } ?: run {
        Log.e("EIdReader", "NfcAdapter is null")
      }
    } catch (e: Exception) {
      Log.e("EIdReader", e.message ?: "Unknown Error")
    }
  }

  override fun onHostPause() {
  }

  override fun onHostDestroy() {
    adapter?.disableForegroundDispatch(currentActivity)
  }

  override fun onActivityResult(p0: Activity?, p1: Int, p2: Int, p3: Intent?) {
  }

  override fun onNewIntent(p0: Intent?) {
    p0?.let { intent ->
      if (!isReading) return

      sendEvent("onTagDiscovered", null)
      currentActivity?.runOnUiThread(Runnable {
        _dialog?.setMessage("ID Document found.")
      })


      if (NfcAdapter.ACTION_TECH_DISCOVERED == intent.action) {
        val tag = intent.extras!!.getParcelable<Tag>(NfcAdapter.EXTRA_TAG)

        if (listOf(*tag!!.techList).contains("android.nfc.tech.IsoDep")) {
          CoroutineScope(Dispatchers.IO).launch {
            try {
              val bacKey: BACKeySpec = BACKey(
                mrzInfo!!.documentNo,
                mrzInfo!!.birthDate,
                mrzInfo!!.expiryDate
              )

              currentActivity?.runOnUiThread(Runnable {
                _dialog?.setMessage("Reading. Hold your document...")
              })

              val result = nfcPassportReader.readPassport(IsoDep.get(tag), bacKey, includeImages, includeRawData)

              val map = result.serializeToMap()
              val reactMap = jsonToReactMap.convertJsonToMap(JSONObject(map))

              stopReading()
              _promise?.resolve(reactMap)
            } catch (e: Exception) {
              reject(e)
            }
          }
        } else {
          reject(Exception("Tag tech is not IsoDep"))
        }
      }
    }
  }

  private fun sendEvent(eventName: String, params: Any?) {
    reactApplicationContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
      .emit(eventName, params)
  }

  private fun reject(e: Exception) {
    stopReading()
    _promise?.reject(e)
  }

  @ReactMethod
  fun startReading(readableMap: ReadableMap?, promise: Promise) {
    readableMap?.let {
      try {
        _promise = promise
        val mrzMap = readableMap?.getMap("mrzInfo")
        val mrzExpirationDate = mrzMap?.getString("expirationDate")
        val mrzBirthDate = mrzMap?.getString("birthDate")
        val mrzDocumentNumber = mrzMap?.getString("documentNumber")

        if (mrzExpirationDate.isNullOrEmpty() || mrzDocumentNumber.isNullOrEmpty() || mrzBirthDate.isNullOrEmpty()) {
          reject(Exception("MRZ info is invalid"))
        }

        mrzInfo = MrzInfo(mrzBirthDate!!, mrzDocumentNumber!!, mrzExpirationDate!!)

        includeImages =
                readableMap.hasKey("includeImages") && readableMap.getBoolean("includeImages")

        includeRawData =
                readableMap.hasKey("includeRawData") && readableMap.getBoolean("includeRawData")

        val currentActivity = currentActivity
        if (currentActivity != null) {
          currentActivity?.runOnUiThread(Runnable {
            val builder = AlertDialog.Builder(currentActivity)
            builder.setTitle("Ready to Scan")
                    .setMessage("Hold your phone near an NFC enabled passport")
                    .setCancelable(true)
                    .setNegativeButton("Cancel") { dialog, _ ->
                      stopReading()
                      val reactMap = jsonToReactMap.convertJsonToMap(JSONObject("{status: 'Canceled' }"))

                      _promise?.resolve(reactMap)
                    }
            _dialog = builder.create()
            _dialog?.show()
          })
        } else {
          promise.reject("ActivityNotFound", "Current activity is null")
        }


        isReading = true
      } catch (e: Exception) {
        reject(Exception("MRZ string is not valid"))
      }
    } ?: run {
      reject(Exception("ReadableMap is null"))
    }
  }

  @ReactMethod
  fun stopReading() {
    isReading = false
    mrzInfo = null
    if (_dialog != null) _dialog?.dismiss()
    _dialog = null
  }

  @ReactMethod
  fun isNfcEnabled(promise: Promise) {
    promise.resolve(NfcAdapter.getDefaultAdapter(reactApplicationContext)?.isEnabled ?: false)
  }

  @ReactMethod
  fun isNfcSupported(promise: Promise) {
    promise.resolve(reactApplicationContext.packageManager.hasSystemFeature(PackageManager.FEATURE_NFC))
  }

  @SuppressLint("QueryPermissionsNeeded")
  @ReactMethod
  fun openNfcSettings(promise: Promise) {
    val intent = Intent(Settings.ACTION_NFC_SETTINGS)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (intent.resolveActivity(reactApplicationContext.packageManager) != null) {
      reactApplicationContext.startActivity(intent)
      promise.resolve(true)
    } else {
      promise.reject(Exception("Activity not found"))
    }
  }

  @ReactMethod
  fun imageDataUrlToJpegDataUrl(dataUrl:String, promise: Promise){
    try {
      val dataSplit = dataUrl.split(";base64,")
      if(dataSplit.size != 2){
        promise.reject("Cannot imageDataUrlToJpegDataUrl image because is not a valid dataurl")
        return
      }
      val mimeType = dataSplit[0].split(":")[1]
      if(!mimeType.startsWith("image/")){
        promise.reject("Couldn't convert $mimeType to JPEG")
        return
      }
      if(mimeType == "image/jpeg"){
        promise.resolve(dataUrl)
        return
      }
      val dataContent = dataSplit[1]
      val bitmapUtil = BitmapUtil(reactApplicationContext)
      val decoded = Base64.decode(dataContent,Base64.DEFAULT)
      val nfcImage = bitmapUtil.getImage(decoded.inputStream(), decoded.size, mimeType)
      if (nfcImage.bitmap != null) {
        val byteArrayOutputStream = ByteArrayOutputStream()
        nfcImage.bitmap!!.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream)
        val bytes = byteArrayOutputStream.toByteArray()
        promise.resolve("data:image/jpeg;base64,"+ Base64.encodeToString(bytes, Base64.CRLF))
        return 
      }
      else promise.reject("Cannot imageDataUrlToJpegDataUrl image")
  
    } catch (e: IOException) {
      promise.reject("Cannot imageDataUrlToJpegDataUrl image")
      return
    }
  }

  companion object {
    const val NAME = "EIdReader"
  }
}
