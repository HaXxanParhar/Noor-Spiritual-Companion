package com.parhar.noor.ui.auth

import android.content.Intent
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.parhar.noor.R
import com.parhar.noor.data.user.UserProfile
import com.parhar.noor.databinding.ActivityLoginBinding
import com.parhar.noor.di.appContainer
import com.parhar.noor.ui.splash.SplashActivity
import com.parhar.noor.utils.BaseActivity
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginActivity : BaseActivity<ActivityLoginBinding>() {

    private val auth = FirebaseAuth.getInstance()
    private val appContainer by lazy { appContainer() }
    private val googleSignInClient: GoogleSignInClient by lazy {
        GoogleSignIn.getClient(this, googleSignInOptions)
    }
    private val googleSignInOptions: GoogleSignInOptions by lazy {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    }
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        handleGoogleSignInResult(result.data)
    }

    override fun inflateBinding(): ActivityLoginBinding =
        ActivityLoginBinding.inflate(layoutInflater)

    override fun setupViews() {
        binding.googleSignInTextView.setOnClickListener {
            signInWithGoogle()
        }

        binding.skipTextView.setOnClickListener {
            openSplash()
        }
    }

    private fun signInWithGoogle() {
        lifecycleScope.launch {
            runCatching {
                googleSignInClient.signOut().await()
            }
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }
    }

    private fun handleGoogleSignInResult(data: Intent?) {
        lifecycleScope.launch {
            runCatching {
                val account = GoogleSignIn.getSignedInAccountFromIntent(data).await()
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                auth.signInWithCredential(credential).await()
            }.onSuccess {
                routeAfterGoogleSignIn()
            }.onFailure { throwable ->
                Toast.makeText(
                    this@LoginActivity,
                    throwable.message ?: "Google sign-in failed.",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    private suspend fun routeAfterGoogleSignIn() {
        val uid = auth.currentUser?.uid.orEmpty()
        if (uid.isBlank()) return

        val profileExists = appContainer.userProfileRepository.userExists(uid)
        if (profileExists) {
            appContainer.userProfileRepository.getUser(uid)?.let { profile ->
                appContainer.sessionManager.saveUserSession(
                    UserProfile(
                        uid = profile.uid,
                        email = profile.email,
                        name = profile.name,
                        gender = profile.gender,
                    ),
                )
            } ?: run {
                val user = auth.currentUser
                appContainer.sessionManager.saveUserSession(
                    UserProfile(
                        uid = uid,
                        email = user?.email.orEmpty(),
                        name = user?.displayName.orEmpty(),
                        gender = "",
                    ),
                )
            }
            openSplash()
        } else {
            openCreateAccount()
        }
    }

    private fun openCreateAccount() {
        startActivity(Intent(this, CreateAccountActivity::class.java))
        finish()
    }

    private fun openSplash() {
        startActivity(SplashActivity.createIntent(this))
        finish()
    }
}
