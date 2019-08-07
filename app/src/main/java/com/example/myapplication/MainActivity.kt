package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.support.v4.app.FragmentActivity
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.view.View.X
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.RelativeLayout
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import java.net.InetAddress
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity(){
    var mRadius:Int = 0
    var mMaxDist:Int = 0
    lateinit var mCenter:PointF

    private lateinit var textureView:TextureView
    private var preview:PreviewCamera? = null
    private val REQUEST_CODE_SIGNIN = 1
    private var drive:Drive? = null
    private val locationList:ArrayList<String> = ArrayList()
    private var locationAdapter: ArrayAdapter<String>? = null
    private lateinit var socketManagement: SocketManagement
    private var DSI_height = 0
    private var DSI_width = 0
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setPermission()
        textureView = camera_texture_view

        setArrayList()
        setSpinner()
        preview = PreviewCamera(this, textureView)
        requestSignIn()
        socketManagement = SocketManagement()
    }

    private fun setAspectRatioTextureView(ResolutionWidth: Int, ResolutionHeight: Int) {
        if (ResolutionWidth > ResolutionHeight) {
            val newWidth = DSI_width
            val newHeight = DSI_width * ResolutionWidth / ResolutionHeight
            updateTextureViewSize(newWidth, newHeight)

        } else {
            val newWidth = DSI_width
            val newHeight = DSI_width * ResolutionHeight / ResolutionWidth
            updateTextureViewSize(newWidth, newHeight)
        }

    }

    private fun updateTextureViewSize(viewWidth: Int, viewHeight: Int) {
        Log.e("test", "TextureView Width : $viewWidth TextureView Height : $viewHeight")
        textureView.layoutParams = FrameLayout.LayoutParams(viewWidth, viewHeight, Gravity.CENTER)
    }

    private fun setSpinner(){
        locationAdapter = ArrayAdapter(this,
            android.R.layout.simple_spinner_dropdown_item,
            locationList)
        locationSpinner.adapter = locationAdapter
    }

    private fun setArrayList(){
        setListItem("영주")
        setListItem("안동")
        setListItem("거창")
        setListItem("안동 경북 사과")
        setListItem("군위")
    }

    private fun setListItem(location:String) = locationList.add(location)

    private fun setPermission() {
        if (permissionGrantedCheck()) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            } else {
                ActivityCompat.requestPermissions(
                    this, arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ), 101
                )
            }
        }
    }


    private fun permissionGrantedCheck() = singlePermissionCheck(Manifest.permission.CAMERA) &&
            singlePermissionCheck(Manifest.permission.READ_EXTERNAL_STORAGE) &&
            singlePermissionCheck(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    private fun singlePermissionCheck(permission: String) =
        ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED

    class DrawOnTop : View {
        constructor(context: Context) : super(context, null)
        @SuppressLint("DrawAllocation")
        override fun onDraw(canvas: Canvas) {
            var paint = Paint()
            paint.isAntiAlias = true
            paint.style = Paint.Style.STROKE
            paint.color = Color.GREEN
            paint.strokeWidth = 10.toFloat()

            var path = Path()
            path.moveTo(750F, 810F)
            path.lineTo(900F, 410F)
            path.lineTo(550F, 110F)
            path.lineTo(200F, 410F)
            path.lineTo(350F, 810F)
            path.close()
            canvas.drawPath(path, paint)
            super.onDraw(canvas)
        }
    }

    override fun onResume() {
        super.onResume()
        preview!!.onResume()
    }

    private fun requestSignIn(){
        val signInOption:GoogleSignInOptions = GoogleSignInOptions.
            Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()
        val client: GoogleSignInClient = GoogleSignIn.getClient(this, signInOption)

        startActivityForResult(client.signInIntent, REQUEST_CODE_SIGNIN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == REQUEST_CODE_SIGNIN){
            handleSignInResult(data)
        }
    }

    private fun handleSignInResult(result:Intent?){
        GoogleSignIn.getSignedInAccountFromIntent(result).addOnSuccessListener { googleSignInAccount ->
            run {
                val credential =
                    GoogleAccountCredential.usingOAuth2(this, Collections.singleton(DriveScopes.DRIVE_FILE))
                credential.selectedAccount = googleSignInAccount.account
                val googleDriveService = Drive.Builder(
                    AndroidHttp.newCompatibleTransport(),
                    GsonFactory(),
                    credential
                ).setApplicationName("Drive API")
                    .build()
                drive = googleDriveService
                preview!!.setDrive(drive)
            }
        }.addOnFailureListener { exception -> Log.e("Unable to Sign In", exception.toString()) }

    }
}



//        camera_card.setOnTouchListener(object : View.OnClickListener, View.OnTouchListener {
//            override fun onClick(v: View?) {
//                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//            }
//
//            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
//                var rect = Rect()
//                v!!.getGlobalVisibleRect(rect)
//
//                mRadius = textureView.width / 2
//                mMaxDist = textureView.height/ 2
//                val x:Float = (rect.left + mRadius).toFloat()
//                val y:Float = (rect.top + mMaxDist).toFloat()
//                mCenter = PointF(x, y)
//
//                var a:Float = event!!.x
//                var b:Float = event.y
//                val c:Float = event.rawX
//                val d:Float = event.rawY
//
//                if ( mCenter.x > event.rawX) {
//                    a = mCenter.x - c + textureView.width / 2
//                }
//                if ( mCenter.y > event.getRawY() ) {
//                    b = mCenter.y - d + textureView.height / 2
//                }
//
//                if ( a > b )
//                    b = a
//                else
//                    a = b
//                val displayMetrics = DisplayMetrics()
//                windowManager.defaultDisplay.getMetrics(displayMetrics)
//                DSI_height = displayMetrics.heightPixels
//                DSI_width = displayMetrics.widthPixels
//
//                setAspectRatioTextureView(a.toInt(), b.toInt())
//
//                return false
//            }
//
//        })
