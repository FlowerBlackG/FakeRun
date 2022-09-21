package com.fakerun.fakerun.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import com.fakerun.fakerun.R
import com.fakerun.fakerun.activity.Home
import com.fakerun.fakerun.tools.GPSUtil
import com.fakerun.fakerun.tools.MockLocationProvider
import org.json.JSONArray
import kotlin.concurrent.thread

class MockRun : Service() {

    companion object {
        val MOCKRUN_ACTION_RUN_START = 1
        val MOCKRUN_ACTION_RUN_END = 2
        //val MOCKRUN_ACTION_RUN_SWITCH = 3
        val MOCKRUN_ACTION_LIFE_CREATE = 11
        val MOCKRUN_ACTION_LIFE_DESTROY = 12
    }

    private val NOTICE_ID_BACKGROUND_SERVICE = 1
    private val NOTICE_ID_LAUNCH_FAILED_BECAUSE_NOT_MOCK_APP = 2

    private val NOTI_CHANNEL_ID_BACKGROUND_SERVICE = "FakeRun Background Service"
    private val NOTI_CHANNEL_NAME_BACKGROUND_SERVICE = "Background Service Guard"

    private val NOTI_CHANNEL_ID_INSTANT_MSG = "FakeRun Ins Msg"
    private val NOTI_CHANNEL_NAME_INSTANT_MSG = "Normal Notification"

    private val checkPoints = ArrayList<Coord>()

    private fun updateCheckPoints(arr: JSONArray) {
        checkPoints.clear()
        for (i in 0 until arr.length()) {
            checkPoints.add(
                Coord(
                    lat = arr.getJSONObject(i).getDouble("lat"),
                    lon = arr.getJSONObject(i).getDouble("lon")
                )
            )
        }
    }

    private var mocking = false

    private lateinit var mNotiManager: NotificationManager
    private lateinit var mNotiBuilder: Notification.Builder

    private lateinit var mockNetwork: MockLocationProvider
    private lateinit var mockGps: MockLocationProvider

    override fun onCreate() {
        super.onCreate()
        Log.d("Service.MockRun", "Life Cycle: on Create")

        initNotification()
    }

    private fun initNotification() {
        mNotiManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        var mNotificationChannel = NotificationChannel(NOTI_CHANNEL_ID_BACKGROUND_SERVICE, NOTI_CHANNEL_NAME_BACKGROUND_SERVICE, NotificationManager.IMPORTANCE_DEFAULT)
        //mNotificationChannel.enableLights(true)
        mNotiManager.createNotificationChannel(mNotificationChannel)

        mNotificationChannel = NotificationChannel(NOTI_CHANNEL_ID_INSTANT_MSG, NOTI_CHANNEL_NAME_INSTANT_MSG, NotificationManager.IMPORTANCE_HIGH)
        mNotificationChannel.enableLights(true)
        //mNotificationChannel.enableVibration(true)
        mNotiManager.createNotificationChannel(mNotificationChannel)

        val push = Intent(this, Home::class.java)
        val contentIntent = PendingIntent.getActivity(this, 0, push, 0)

        mNotiBuilder = Notification.Builder(this, NOTI_CHANNEL_ID_BACKGROUND_SERVICE)
        mNotiBuilder.setContentTitle("跑吗后台服务")
                .setContentText("戳\"启动\"以开始～")
                .setContentIntent(contentIntent)
                //.setTicker("后台服务准备就绪～")
                .setOnlyAlertOnce(true)
                .setSmallIcon(R.drawable.fakerun_noti_icon)
        startForeground(NOTICE_ID_BACKGROUND_SERVICE, mNotiBuilder.build())
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("Service.MockRun", "Life Cycle: on Start Command")

        when (intent!!.getIntExtra("action", -100)) {
            MOCKRUN_ACTION_RUN_START -> {
                if (!mocking) {
                    speed = intent.getDoubleExtra("speed", 1.0)
                    deltaT = intent.getIntExtra("deltaT", 1000)
                    val mapPointArr = JSONArray(intent.getStringExtra("mapData"))
                    updateCheckPoints(mapPointArr)
                    try {
                        initProviders()
                        mocking = true
                        createMockThread()
                    } catch (e: Exception) {
                        Toast.makeText(this, "失败原因：\n$e", Toast.LENGTH_LONG).show()
                        updateNotiText("启动失败。请将本app设为系统模拟定位应用")
                        val push = Intent(this, Home::class.java)
                        val contentIntent = PendingIntent.getActivity(this, 0, push, 0)
                        val noti = Notification.Builder(this, NOTI_CHANNEL_ID_INSTANT_MSG)
                                .setContentText("启动失败。请将本app设为系统模拟定位应用")
                                .setSmallIcon(R.drawable.fakerun_noti_icon)
                                .setContentIntent(contentIntent)
                                .setTicker("启动失败。请将本app设为系统模拟定位应用")
                                .setAutoCancel(true)
                                .build()
                        mNotiManager.notify(NOTICE_ID_LAUNCH_FAILED_BECAUSE_NOT_MOCK_APP, noti)
                    }
                } else {
                    val push = Intent(this, Home::class.java)
                    val contentIntent = PendingIntent.getActivity(this, 0, push, 0)
                    val noti = Notification.Builder(this, NOTI_CHANNEL_ID_INSTANT_MSG)
                            .setContentText("服务已在运行，无需再启动")
                            .setSmallIcon(R.drawable.fakerun_noti_icon)
                            .setContentIntent(contentIntent)
                            .setTicker("服务已在运行，无需再启动")
                            .setAutoCancel(true)
                            .build()
                    mNotiManager.notify(NOTICE_ID_LAUNCH_FAILED_BECAUSE_NOT_MOCK_APP, noti)
                }
            }
            MOCKRUN_ACTION_RUN_END -> {
                if (mocking) {
                    mocking = false
                    shutdownProviders()
                    updateNotiText("就绪～")
                } else {
                    val push = Intent(this, Home::class.java)
                    val contentIntent = PendingIntent.getActivity(this, 0, push, 0)
                    val noti = Notification.Builder(this, NOTI_CHANNEL_ID_INSTANT_MSG)
                            .setContentText("服务未在运行")
                            .setSmallIcon(R.drawable.fakerun_noti_icon)
                            .setContentIntent(contentIntent)
                            .setTicker("服务未在运行")
                            .setTimeoutAfter(6000L)
                            .setAutoCancel(true)
                            .build()
                    mNotiManager.notify(NOTICE_ID_LAUNCH_FAILED_BECAUSE_NOT_MOCK_APP, noti)
                }
            }
            MOCKRUN_ACTION_LIFE_CREATE -> {
                // no action
            }
            MOCKRUN_ACTION_LIFE_DESTROY -> {
                stopSelf()
            }
        }

        mNotiManager.notify(NOTICE_ID_BACKGROUND_SERVICE, mNotiBuilder.build())

        return START_STICKY
        //return super.onStartCommand(intent, flags, startId)
    }

    private var speed = 1.0 // meters per second
    private var deltaT = 1 // ms
    private fun createMockThread() {
        thread {
            val checkPointsCount = checkPoints.size
            var pointA: Coord
            var pointB: Coord
            var i = -1
            while (mocking) {
                i++
                pointA = checkPoints[i % checkPointsCount]
                pointB = checkPoints[(i + 1) % checkPointsCount]

                val deltaLat = pointB.lat - pointA.lat
                val deltaLon = pointB.lon - pointA.lon

                val distance = GPSUtil.distanceByLongNLat(pointA.lon, pointA.lat, pointB.lon, pointB.lat)
                val steps = (distance / speed / (deltaT / 1000.0)).toLong()

                for (i in 1..steps) {
                    if (!mocking) {
                        return@thread
                    }

                    Thread.sleep(deltaT.toLong())

                    val tarCoord = Coord(1.0, 1.0)
                    tarCoord.lat = pointA.lat + deltaLat * i / steps + ((999985..1000030).random() / 1000000.0 - 1)
                    tarCoord.lon = pointA.lon + deltaLon * i / steps + ((999985..1000030).random() / 1000000.0 - 1)

                    try {
                        updateLocation(tarCoord)
                    } catch (e: Exception) {

                    }

                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.d("Service.MockRun", "Life Cycle: on Bind")
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("Service.MockRun", "Life Cycle: on Destroy")

        if (mocking) {
            shutdownProviders()
            mocking = false
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun initProviders() {
        mockNetwork = MockLocationProvider(LocationManager.NETWORK_PROVIDER, this)
        mockGps = MockLocationProvider(LocationManager.GPS_PROVIDER, this)
    }

    private fun shutdownProviders() {
        mockNetwork.shutdown()
        mockGps.shutdown()
    }

    private fun updateLocation(lat: Double, lon: Double, isWgs84: Boolean = false) {
        val coord = Coord(lat = lat, lon = lon)
        if (!isWgs84) {
            val wgs84loc = GPSUtil.gcj02_To_Gps84(lat, lon)
            coord.lat = wgs84loc[0]
            coord.lon = wgs84loc[1]
        }

        val notiText = String.format("纬度: %.6f\n经度: %.6f\n坐标系统：GCJ02", coord.lat, coord.lon)
        updateNotiText(notiText)

        mockNetwork.pushLocation(coord.lat + ((999996..1000005).random() / 1000000.0 - 1), coord.lon + ((999996..1000005).random() / 1000000.0 - 1))
        mockGps.pushLocation(coord.lat, coord.lon)
    }

    private fun updateNotiText(str: String) {
        mNotiBuilder.setContentText(str)
        //startForeground(NOTICE_ID_BACKGROUND_SERVICE, mNotiBuilder.build())
        mNotiManager.notify(NOTICE_ID_BACKGROUND_SERVICE, mNotiBuilder.build())
    }

    private fun updateLocation(coord: Coord, isWgs84: Boolean = false) {
        Log.d("Home.updateLocation", "lat=${coord.lat}, lon=${coord.lon}")
        updateLocation(coord.lat, coord.lon, isWgs84)
    }

    data class Coord(var lon: Double, var lat: Double)
}
