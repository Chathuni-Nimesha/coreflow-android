package com.chathuninimesha.coreflow.ui.auth

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.chathuninimesha.coreflow.R
import com.chathuninimesha.coreflow.auth.AuthError
import com.chathuninimesha.coreflow.auth.AuthModule
import com.chathuninimesha.coreflow.auth.AuthNavigator
import com.chathuninimesha.coreflow.auth.AuthResult
import com.chathuninimesha.coreflow.auth.toMessageRes
import com.chathuninimesha.coreflow.databinding.ActivitySigninBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SignInActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySigninBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySigninBinding.inflate(layoutInflater)
        setContentView(binding.root)
        AuthModule.init(this)

        binding.goToSignUp.setOnClickListener { AuthNavigator.openSignUp(this) }
        binding.loginBtn.setOnClickListener { submit() }
    }

    private fun submit() {
        if (!binding.loginBtn.isEnabled) return
        binding.emailInputLayout.error = null
        binding.passwordInputLayout.error = null
        val email = binding.emailEditText.text?.toString().orEmpty()
        val password = binding.passwordEditText.text?.toString()?.toCharArray() ?: CharArray(0)

        binding.loginBtn.isEnabled = false
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.Default) {
                    AuthModule.repository().signIn(email, password)
                }
                binding.passwordEditText.text?.clear()
                when (result) {
                    AuthResult.Success -> AuthNavigator.openMain(this@SignInActivity)
                    is AuthResult.Failure -> showFailure(result.error)
                }
            } finally {
                if (!isFinishing) binding.loginBtn.isEnabled = true
            }
        }
    }

    private fun showFailure(error: AuthError) {
        val message = getString(error.toMessageRes())
        when (error) {
            AuthError.EMAIL_REQUIRED, AuthError.EMAIL_INVALID, AuthError.UNKNOWN_EMAIL ->
                binding.emailInputLayout.error = message
            AuthError.PASSWORD_REQUIRED, AuthError.WRONG_PASSWORD ->
                binding.passwordInputLayout.error = message
            else -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }
}
