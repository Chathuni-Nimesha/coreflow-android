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
import com.chathuninimesha.coreflow.databinding.ActivitySignUpBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        AuthModule.init(this)

        binding.goToSignIn.setOnClickListener { AuthNavigator.openSignIn(this) }
        binding.continueBtn.setOnClickListener { submit() }
    }

    private fun submit() {
        if (!binding.continueBtn.isEnabled) return
        clearErrors()
        val name = binding.nameEditText.text?.toString().orEmpty()
        val email = binding.emailEditText.text?.toString().orEmpty()
        val password = binding.passwordEditText.text?.toString()?.toCharArray() ?: CharArray(0)
        val confirm = binding.confirmPasswordEditText.text?.toString()?.toCharArray() ?: CharArray(0)

        binding.continueBtn.isEnabled = false
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.Default) {
                    AuthModule.repository().signUp(name, email, password, confirm)
                }
                binding.passwordEditText.text?.clear()
                binding.confirmPasswordEditText.text?.clear()
                when (result) {
                    AuthResult.Success -> AuthNavigator.openMain(this@SignUpActivity)
                    is AuthResult.Failure -> showFailure(result.error)
                }
            } finally {
                if (!isFinishing) binding.continueBtn.isEnabled = true
            }
        }
    }

    private fun showFailure(error: AuthError) {
        val message = getString(error.toMessageRes())
        when (error) {
            AuthError.NAME_REQUIRED -> binding.nameInputLayout.error = message
            AuthError.EMAIL_REQUIRED,
            AuthError.EMAIL_INVALID,
            AuthError.DUPLICATE_EMAIL,
            AuthError.PROFILE_EXISTS ->
                binding.emailInputLayout.error = message
            AuthError.PASSWORD_REQUIRED, AuthError.PASSWORD_TOO_SHORT ->
                binding.passwordInputLayout.error = message
            AuthError.CONFIRM_PASSWORD_REQUIRED, AuthError.PASSWORD_MISMATCH ->
                binding.confirmPasswordInputLayout.error = message
            else -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearErrors() {
        binding.nameInputLayout.error = null
        binding.emailInputLayout.error = null
        binding.passwordInputLayout.error = null
        binding.confirmPasswordInputLayout.error = null
    }
}
