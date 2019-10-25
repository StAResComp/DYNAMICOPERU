package uk.ac.standrews.pescar

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

import net.openid.appauth.AuthState
import java.util.*
import org.json.JSONException
import android.text.TextUtils
import androidx.annotation.Nullable
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationService
import android.app.PendingIntent
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Environment
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_auth.navigation
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import android.Manifest
import android.app.AlertDialog
import android.media.MediaScannerConnection
import android.widget.ProgressBar

class AuthActivity : AppCompatActivity() {

    private val SHARED_PREFERENCES_NAME = "AuthStatePreference"
    private val AUTH_STATE = "AUTH_STATE"
    private val USED_INTENT = "USED_INTENT"
    private val PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE = 6954

    private lateinit var authBtn: Button
    private lateinit var authText: TextView
    private var authState: AuthState? = null

    private lateinit var exportBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)
        authBtn = findViewById(R.id.auth_button)
        authText = findViewById(R.id.auth_text)
        (navigation as BottomNavigationView).setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
        (navigation as BottomNavigationView).menu.findItem(R.id.navigation_link).isChecked = true

        enablePostAuthorizationFlows()

        authBtn.setOnClickListener {view ->
            val serviceConfiguration = AuthorizationServiceConfiguration(
                Uri.parse(getString(R.string.pescar_authorize_url)) /* auth endpoint */,
                Uri.parse(getString(R.string.pescar_token_url)) /* token endpoint */
            )
            val clientId = getString(R.string.pescar_client_id)
            val redirectUri = Uri.parse(getString(R.string.pescar_auth_redirect_uri))
            val builder = AuthorizationRequest.Builder(
                serviceConfiguration,
                clientId,
                "code",
                redirectUri
            )
            //builder.setScopes("profile")
            val request = builder.build()
            val authorizationService = AuthorizationService(view.context)
            val action = "uk.ac.standrews.pescar.HANDLE_AUTHORIZATION_RESPONSE"
            val postAuthorizationIntent = Intent(view.context, AuthActivity::class.java)
            postAuthorizationIntent.action = action
            val pendingIntent = PendingIntent.getActivity(view.context, request.hashCode(), postAuthorizationIntent, 0)
            authorizationService.performAuthorizationRequest(request, pendingIntent)
        }

        exportBtn = findViewById(R.id.export_button)
        exportBtn.setOnClickListener{
            val simpleProgressBar = findViewById<ProgressBar>(R.id.simpleProgressBar)
            simpleProgressBar.visibility = View.VISIBLE
            if (ContextCompat.checkSelfPermission(this@AuthActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this@AuthActivity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE)
            }
            else {
                this@AuthActivity.exportData()
            }
            simpleProgressBar.visibility = View.INVISIBLE

        }
    }

    private fun enablePostAuthorizationFlows() {
        authState = restoreAuthState()
        val currAuthState = authState
        if (currAuthState != null && currAuthState.isAuthorized) {
            authText.setText(R.string.already_authorized)
        }
        else {
            authText.setText(R.string.please_authorize)
        }
    }

    private fun handleAuthorizationResponse(intent: Intent) {
        Log.i("OAuth", "Handling response")
        val response = AuthorizationResponse.fromIntent(intent)
        val error = AuthorizationException.fromIntent(intent)
        val authState = AuthState(response, error)
        if (response != null) {
            Log.i("OAuth", String.format("Handled Authorization Response %s ", authState.jsonSerializeString()))
            val service = AuthorizationService(this)
            service.performTokenRequest(response.createTokenExchangeRequest()
            ) { tokenResponse, exception ->
                if (exception != null) {
                    Log.w("OAuth", "Token Exchange failed", exception)
                } else {
                    if (tokenResponse != null) {
                        authState.update(tokenResponse, exception)
                        persistAuthState(authState)
                        Log.i("OAuth", String.format("Token Response [ Access Token: %s, ID Token: %s ]", tokenResponse.accessToken, tokenResponse.idToken))
                    }
                }
            }
        }
    }

    private fun persistAuthState(authState: AuthState) {
        getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).edit()
            .putString(AUTH_STATE, authState.jsonSerializeString())
            .commit()
        enablePostAuthorizationFlows()
    }

    private fun clearAuthState() {
        getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(AUTH_STATE)
            .apply()
    }

    @Nullable
    private fun restoreAuthState(): AuthState? {
        val jsonString = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
            .getString(AUTH_STATE, null)
        if (!TextUtils.isEmpty(jsonString)) {
            try {
                return AuthState.jsonDeserialize(jsonString)
            } catch (jsonException: JSONException) {
                // should never happen
            }

        }
        return null
    }

    //Handle navigation
    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_today -> {
                val intent = Intent(this, TodayActivity::class.java)
                startActivity(intent)
            }
            R.id.navigation_archive -> {
                val cal = Calendar.getInstance()
                val dpd = DatePickerDialog(this@AuthActivity, DatePickerDialog.OnDateSetListener { _, year, month, day ->
                    val intent = Intent(this, ArchiveActivity::class.java)
                    val picked = Calendar.getInstance()
                    picked.set(Calendar.YEAR, year)
                    picked.set(Calendar.MONTH, month)
                    picked.set(Calendar.DAY_OF_MONTH, day)
                    picked.set(Calendar.HOUR_OF_DAY, 0)
                    picked.set(Calendar.MINUTE, 0)
                    picked.set(Calendar.SECOND, 0)
                    picked.set(Calendar.MILLISECOND, 0)
                    intent.putExtra("midnight", picked.timeInMillis)
                    startActivity(intent)
                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
                dpd.show()
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_link -> {
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    override fun onNewIntent(intent: Intent) {
        checkIntent(intent)
        super.onNewIntent(intent)
    }

    private fun checkIntent(intent: Intent?) {
        if (intent != null) {
            val action = intent.action
            when (action) {
                "uk.ac.standrews.pescar.HANDLE_AUTHORIZATION_RESPONSE" -> if (!intent.hasExtra(USED_INTENT)) {
                    handleAuthorizationResponse(intent)
                    intent.putExtra(USED_INTENT, true)
                }
            }// do nothing
        }
    }

    override fun onStart() {
        super.onStart()
        checkIntent(intent)
    }

    private fun exportData() {

        val exportDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "pescar_export_"+Calendar.getInstance().timeInMillis)
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }
        val db = AppDatabase.getAppDataBase(applicationContext).openHelper.readableDatabase

        val posFile = File(exportDir, "positions.csv")
        val writer = BufferedWriter(FileWriter(posFile))

        val positions = db.query("SELECT * FROM position")
        writer.write(implode(positions.columnNames))
        while (positions.moveToNext()) {
            writer.newLine()
            writer.write(getRow(positions))
        }
        writer.close()
        positions.close()

        MediaScannerConnection.scanFile(this@AuthActivity, arrayOf(posFile.absolutePath), arrayOf("text/csv"), null)

        confirmExport(posFile)
    }

    private fun confirmExport(file:File) {
        val alertDialog = AlertDialog.Builder(this@AuthActivity)
        alertDialog.setTitle(getString(R.string.export_dialog_title))
        alertDialog.setMessage(getString(R.string.export_dialog_message))
        alertDialog.setPositiveButton(getString(R.string.email_now)) {_, _ ->
            emailFile(file)
        }
        alertDialog.setNegativeButton(getString(R.string.cancel)) {dialog, _ ->
            dialog.cancel()
        }
        alertDialog.show()
    }

    private fun emailFile(file:File) {
        val emailIntent = Intent()
        emailIntent.action = Intent.ACTION_SEND
        emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        emailIntent.type = "vnd.android.cursor.dir/email"
        emailIntent.putExtra(Intent.EXTRA_STREAM, GenericFileProvider.getUriForFile(
            this, "uk.ac.standrews.pescar", file))
        startActivityForResult(emailIntent, 101)
    }

    private fun implode(data:Array<String>, separator:String=","):String {
        val sb = StringBuilder()
        for (i in data.indices) {
            sb.append(data[i])
            if (i < data.size - 1) {
                sb.append(separator)
            }
        }
        return sb.toString()
    }

    private fun getRow(data: Cursor):String {
        var dataArray = arrayOf<String>()
        for (i in 0 until data.columnCount-1) {
            dataArray += data.getString(i)
        }
        return implode(dataArray)
    }
}
