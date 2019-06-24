package uk.ac.standrews.pescar

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView

import kotlinx.android.synthetic.main.activity_auth.*
import net.openid.appauth.AuthState
import java.util.*
import org.json.JSONException
import android.text.TextUtils
import androidx.annotation.Nullable
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationService
import android.app.PendingIntent
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse
import android.util.Log
import android.view.View
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_auth.navigation
import kotlinx.android.synthetic.main.activity_today.*


class AuthActivity : AppCompatActivity() {

    private val SHARED_PREFERENCES_NAME = "AuthStatePreference"
    private val AUTH_STATE = "AUTH_STATE"
    private val USED_INTENT = "USED_INTENT"

    private lateinit var authBtn: Button
    private lateinit var authText: TextView
    private var authState: AuthState? = null

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
}
